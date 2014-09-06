package firelib.execution

import java.time.Instant

import firelib.common.{TradeGateCallback, _}
import firelib.common.marketstub.MarketStub
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer


class ExecutionMarketStub(val tradeGate: TradeGate, val security_ : String, val maxOrderCount: Int = 20) extends MarketStub with TradeGateCallback {

    tradeGate.registerCallback(this)

    private val bidAsk = Array(Double.NaN, Double.NaN)
    private var dtGmt: Instant = _

    private val orders_ = new ArrayBuffer[Order]()

    private val tradeGateCallbacks = new ArrayBuffer[TradeGateCallback]()

    private var position_ = 0

    private val trades_ = new ArrayBuffer[Trade]()

    private var orderIdCnt = 0

    private val log = LoggerFactory.getLogger(getClass)


    def trades: Seq[Trade] = trades_

    def orders: Seq[Order] = orders_

    override def hasPendingState: Boolean = {
        orders_.exists(o => (o.status.isPending || (o.orderType == OrderType.Market)))
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


    def cancelAllOrders(): Unit = cancelOrderByIds(orders_.map(_.id))


    def cancelOrderByIds(orderIds: Seq[String]): Unit = {

        for (orderId <- orderIds) {
            orders_.find(_.id == orderId) match {
                case Some(ord) => {
                    tradeGate.cancelOrder(orderId)
                    ord.status = OrderStatus.PendingCancel
                    tradeGateCallbacks.foreach(_.onOrderStatus(ord, OrderStatus.PendingCancel))

                }
                case None => log.error(s"cancelling non existing order $orderId")
            }
        }
    }


    def submitOrders(orders: Seq[Order]) = {

        assert(this.orders_.length <= maxOrderCount, "max order count exceeded")

        orders.foreach(order => {
            order.id = security_ + "" + (orderIdCnt += 1)
            order.placementTime = dtGmt
            order.security = security_
            fireOrderState(List(order), OrderStatus.New)
            if (order.minutesToHold != -1) {
                order.validUntil = dtGmt.plusSeconds(order.minutesToHold * 60)
            }
            this.orders_ += order
            log.info(s"submitting order $order")
            tradeGate.sendOrder(order)
        })
    }

    def fireOrderState(list: Traversable[Order], orderStatus: OrderStatus) = {
        list.foreach(order => tradeGateCallbacks.foreach(tgc => tgc.onOrderStatus(order, orderStatus)))
    }


    def closePosition(reason: Option[String]): Unit = {
        if (position_ == 0 || hasPendingState)
            return

        val order: Order = new Order(OrderType.Market, 0, math.abs(position_), Side.sideForAmt(-position_)) {
            security = security_
        }
        submitOrders(List(order))
    }

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt: Instant) {
        bidAsk(0) = bid
        bidAsk(1) = ask
        this.dtGmt = dtGmt
    }


    def onOrderStatus(order: Order, status: OrderStatus): Unit = {
        if (!orders_.exists(o => o.id == order.id)) {
            return
        }
        log.info(s"order status $order status $status")

        if (status.isFinal) {
            orders_.remove(orders_.indexWhere(o => o.id == order.id))
        }
        tradeGateCallbacks.foreach(tg => tg.onOrderStatus(order, status))
    }

    def onTrade(trd: Trade): Unit = {
        if (!orders_.exists(_.id == trd.order.id)) {
            return
        }
        log.info(s"on trade $trd")
        trades_ += trd
        position_ = trd.adjustPositionByThisTrade(position_)
        trd.positionAfter = position_
        log.info(s"position after $position")
        tradeGateCallbacks.foreach(tgc => tgc.onTrade(trd))
    }

    override def position: Int = position_
    override val security: String = security_
}
