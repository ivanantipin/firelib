package firelib.common.marketstub

import java.time.Instant

import firelib.common.{TradeGateCallback, _}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.collection.{Map, mutable}



class OrderManagerImpl(var tradeGate: TradeGate, val security : String, val maxOrderCount: Int = 20) extends OrderManager with TradeGateCallback {

    private var tradeGateSubscription: DisposableSubscription = tradeGate.registerCallback(this)

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

    var idCounter = System.currentTimeMillis()

    def trades: Seq[Trade] = trades_

    var logEnabled = false

    def liveOrders: Seq[Order] = id2Order.values.to[Seq]

    override def hasPendingState: Boolean = {
        id2Order.values.exists(o => (o.status.isPending || (o.orderType == OrderType.Market)))
    }

    def replaceTradeGate(tg : TradeGate): Unit ={
        assert(id2Order.size == 0, "orders count on stubs switching should be 0")
        assert(position == 0, "position on stubs switching should be 0")
        tradeGateSubscription.unsubscribe()
        tradeGateSubscription = tg.registerCallback(this)
        tradeGate = tg
    }


    def addCallback(callback: TradeGateCallback) = {
        tradeGateCallbacks += callback
    }

    def cancelOrderByIds(orderIds: String*): Unit = {
        for (orderId <- orderIds) {
            id2Order.get(orderId) match {
                case Some(ord) => {
                    tradeGate.cancelOrder(orderId)
                    ord.statuses += OrderStatus.PendingCancel
                    tradeGateCallbacks.foreach(_.onOrderStatus(ord, OrderStatus.PendingCancel))
                }

                case None => if(logEnabled) log.error(s"cancelling non existing order $orderId")
            }
        }
    }


    def submitOrders(orders: Order*) = {
        if(this.id2Order.size > maxOrderCount){
            if(logEnabled) log.error(s"max order count reached rejecting orders $orders")
            fireOrderState(orders, OrderStatus.Rejected)
        }else{
            orders.foreach(order => {
                order.placementTime = dtGmt
                fireOrderState(List(order), OrderStatus.New)
                this.id2Order(order.id) = order
                if(logEnabled) log.info(s"submitting order $order")
                tradeGate.sendOrder(order)
            })

        }
    }

    def fireOrderState(list: Traversable[Order], orderStatus: OrderStatus) = {
        list.foreach(order => tradeGateCallbacks.foreach(tgc => tgc.onOrderStatus(order, orderStatus)))
    }


    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt: Instant) {
        bidAsk(0) = bid
        bidAsk(1) = ask
        this.dtGmt = dtGmt
    }


    def onOrderStatus(order: Order, status: OrderStatus): Unit = {
        if(id2OrderFinalized.contains(order.id)){
            if(logEnabled) log.error(s"order status $status received for completed order $order ")
            return
        }
        if (!id2Order.contains(order.id)) {
            return
        }
        if(logEnabled) log.info(s"order status $order status $status")

        order.statuses += status

        if (status.isFinal) {
            if(status == OrderStatus.Done && order.remainingQty > 0){
                if(logEnabled) log.error(s"status is Done but order $order has non zero remaining amount ${order.remainingQty} ")
            }
            val finalOrder: Order = id2Order.remove(order.id).get
            id2OrderFinalized(finalOrder.id) = finalOrder
        }
        tradeGateCallbacks.foreach(tg => tg.onOrderStatus(order, status))
    }

    private def procTrade(trd : Trade, ords : Map[String,Order]) : Boolean = {
        ords.get(trd.order.id) match {
            case Some(order) =>{
                if(logEnabled) log.info(s"on trade $trd")
                trades_ += trd
                order.trades += trd
                if(logEnabled && order.remainingQty < 0){
                    log.error(s"negative remaining amount order $order")
                }
                val prevPos = position_
                position_ = trd.adjustPositionByThisTrade(position_)
                trd.positionAfter = position_
                if(logEnabled) log.info(s"position adjusted for security $security :  $prevPos -> $position")
                tradeGateCallbacks.foreach(tgc => tgc.onTrade(trd))
                true
            }
            case None =>false
        }
    }

    def onTrade(trd: Trade): Unit = {
        if(!procTrade(trd, id2Order)){
            if(procTrade(trd,id2OrderFinalized)){
                if(logEnabled) log.error(s"trade received for finilized order $trd order ${id2OrderFinalized(trd.order.id)} ")
            }
        }

    }


    override def nextOrderId: String = {
        idCounter+= 1
        return s"${security}_${idCounter}"
    }
}
