package firelib.execution

import java.time.Instant

import firelib.common.marketstub.MarketStub
import firelib.common.{TradeGateCallback, _}
import org.slf4j.LoggerFactory

import scala.collection.{Map, mutable}
import scala.collection.mutable.ArrayBuffer



class ExecutionMarketStub(val tradeGate: TradeGate, val security_ : String, val maxOrderCount: Int = 20) extends MarketStub with TradeGateCallback {

    tradeGate.registerCallback(this)

    private val bidAsk = Array(Double.NaN, Double.NaN)
    private var dtGmt: Instant = _

    private val id2Order = new mutable.HashMap[String,Order]()

    private val id2OrderFinalized = new mutable.HashMap[String,Order]()

    private val tradeGateCallbacks = new ArrayBuffer[TradeGateCallback]()

    private var position_ = 0

    private val trades_ = new ArrayBuffer[Trade]()

    private var orderIdCnt = 0

    private val log = LoggerFactory.getLogger(getClass)

    override def position: Int = position_
    override val security: String = security_

    var idCounter = System.currentTimeMillis()


    def trades: Seq[Trade] = trades_

    def orders: Iterable[Order] = id2Order.values

    override def hasPendingState: Boolean = {
        id2Order.values.exists(o => (o.status.isPending || (o.orderType == OrderType.Market)))
    }


    def addCallback(callback: TradeGateCallback) = {
        tradeGateCallbacks += callback
    }

    def moveCallbacksTo(marketStub: MarketStub) = {
        tradeGateCallbacks.foreach(marketStub.addCallback)
        tradeGateCallbacks.clear()
    }

    def flattenAll(reason: Option[String]) = {
        cancelAllOrders()
        closePosition(reason)
    }


    def cancelAllOrders(): Unit = cancelOrderByIds(id2Order.keys.toList: _*)


    def cancelOrderByIds(orderIds: String*): Unit = {
        for (orderId <- orderIds) {
            id2Order.get(orderId) match {
                case Some(ord) => {
                    tradeGate.cancelOrder(orderId)
                    ord.statuses += OrderStatus.PendingCancel
                    tradeGateCallbacks.foreach(_.onOrderStatus(ord, OrderStatus.PendingCancel))

                }
                case None => log.error(s"cancelling non existing order $orderId")
            }
        }
    }


    def submitOrders(orders: Order*) = {
        if(this.id2Order.size > maxOrderCount){
            log.error(s"max order count reached rejecting orders $orders")
            fireOrderState(orders, OrderStatus.Rejected)
        }else{
            orders.foreach(order => {
                order.placementTime = dtGmt
                fireOrderState(List(order), OrderStatus.New)
                this.id2Order(order.id) = order
                log.info(s"submitting order $order")
                tradeGate.sendOrder(order)
            })

        }
    }

    def fireOrderState(list: Traversable[Order], orderStatus: OrderStatus) = {
        list.foreach(order => tradeGateCallbacks.foreach(tgc => tgc.onOrderStatus(order, orderStatus)))
    }


    def closePosition(reason: Option[String]): Unit = {
        if (position_ == 0){
            return
        }
        if (hasPendingState){
            log.error("Ignoring closing position request as pending position exists")
            return
        }
        val order: Order = new Order(OrderType.Market, 0, math.abs(position_), Side.sideForAmt(-position_), security, nextOrderId)
        submitOrders(order)
    }

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt: Instant) {
        bidAsk(0) = bid
        bidAsk(1) = ask
        this.dtGmt = dtGmt
    }


    def onOrderStatus(order: Order, status: OrderStatus): Unit = {
        if(id2OrderFinalized.contains(order.id)){
            log.error(s"order status $status received for completed order $order ")
            return
        }
        if (!id2Order.contains(order.id)) {
            return
        }
        log.info(s"order status $order status $status")

        if (status.isFinal) {
            if(status == OrderStatus.Done && order.remainingQty > 0){
                log.error(s"status is Done but order $order has non zero remaining amount ${order.remainingQty} ")
            }
            val finalOrder: Order = id2Order.remove(order.id).get
            id2OrderFinalized(finalOrder.id) = finalOrder
        }
        tradeGateCallbacks.foreach(tg => tg.onOrderStatus(order, status))
    }

    private def procTrade(trd : Trade, ords : Map[String,Order]) : Boolean = {
        ords.get(trd.order.id) match {
            case Some(order) =>{
                log.info(s"on trade $trd")
                trades_ += trd
                order.trades += trd
                if(order.remainingQty < 0){
                    log.error(s"negative remaining amount order $order")
                }
                val prevPos = position_
                position_ = trd.adjustPositionByThisTrade(position_)
                trd.positionAfter = position_
                log.info(s"position adjusted for security $security :  $prevPos -> $position")
                tradeGateCallbacks.foreach(tgc => tgc.onTrade(trd))
                true
            }
            case None =>false
        }
    }

    def onTrade(trd: Trade): Unit = {
        if(!procTrade(trd, id2Order)){
            if(procTrade(trd,id2OrderFinalized)){
                log.error(s"trades ")
            }
        }

    }


    override def nextOrderId: String = {
        idCounter+= 1
        return s"${security}_${idCounter}"
    }
}
