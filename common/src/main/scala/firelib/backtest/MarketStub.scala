package firelib.backtest

import java.time.Instant

import firelib.common._

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class MarketStub(val security: String, val maxOrderCount: Int = 20) extends IMarketStub {

    var bid = Double.NaN

    var ask = Double.NaN

    var dtGmt: Instant  =_

    val orders_ = new mutable.HashMap[String, Order]()

    def orders = orders_.values.toArray[Order]

    val tradeGateCallbacks = ArrayBuffer[ITradeGateCallback]()

    var position_ = 0

    val trades = new ListBuffer[Trade]()

    var orderIdCnt = 0

    var lastPositionTrade: Trade = _

    val hasPendingState = false


    private def middlePrice: Double = (bid + ask) / 2


    def addCallback(callback: ITradeGateCallback) = tradeGateCallbacks += callback


    def moveCallbacksTo(marketStub: IMarketStub) : Unit = {
        tradeGateCallbacks.foreach(marketStub.addCallback)
        tradeGateCallbacks.clear()
    }

    def flattenAll(reason: Option[String]) : Unit = {
        cancelAllOrders()
        closePosition(reason)
    }

    def cancelAllOrders()  : Unit = {
        val ords = orders_.values.toArray
        orders_.clear()
        ords.foreach(o=>fireOrderState(o,OrderStatus.Cancelled))
    }

    def cancelOrderByIds(orderIds: Seq[String])  : Unit = {
        orderIds.foreach(orderId => {
            val ord = orders_.remove(orderId)
            assert(ord.isDefined,"no order with id " + orderId)
            fireOrderState(ord.get, OrderStatus.Cancelled)
        })
    }

    private def fireTradeEvent(trade: Trade) = tradeGateCallbacks.foreach(tgc => tgc.onTrade(trade))

    def submitOrders(orders: Seq[Order]) : Unit = {

        assert(this.orders_.size + orders.length <= maxOrderCount, "max order count exceeded")

        orders.foreach(order => {
            order.id = nextOrderId()
            order.placementTime = dtGmt
            order.security = security
            if (order.minutesToHold != -1) {
                order.validUntil = dtGmt.plusSeconds(order.minutesToHold*60)
            }
            this.orders_(order.id) = order
            fireOrderState(order, OrderStatus.New)
        })
        checkOrders()
    }

    private def nextOrderId() : String = {
        orderIdCnt+= 1
        security + orderIdCnt
    }


    def closePosition(reason: Option[String]): Unit = {
        if (position_ == 0)
            return

        val ord = new Order(OrderType.Market, 0, math.abs(position_), Side.sideForAmt(-position_)) {
            security = security
        }
        val trd = new Trade(math.abs(position_), checkOrderAndGetTradePrice(ord).get, ord.side, ord, dtGmt,
            security) {
            reason = reason
        }
        onTrade(trd)

        assert(position_ == 0,"position must be 0 after flatten all!!!")
    }

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant) = {
        this.bid = bid
        this.ask = ask
        if (position_ != 0) {
            lastPositionTrade.onPrice(if (lastPositionTrade.side == Side.Buy) bid else ask)
        }
        this.dtGmt = dtGmt
        checkOrders()
    }

    def checkOrders() : Unit = {
        orders_ retain ((id, ord) => !checkExecutionWithStateTrigger(ord).isFinal)
    }

    def checkExecutionWithStateTrigger(ord: Order) : OrderStatus = {
        chkOrderExecution(ord) match {
            case Some(trd) => {
                fireOrderState(ord, OrderStatus.Done)
                return OrderStatus.Done
            }
            case None => {
                assert(ord.orderType != OrderType.Market, "market order should cause position change!!")
                if (ord.validUntil != null && ord.validUntil.isBefore(dtGmt)) {
                    fireOrderState(ord, OrderStatus.Cancelled)
                    return OrderStatus.Cancelled
                }

                //FIXME
                return OrderStatus.Accepted
            }
        }
    }

    private def fireOrderState(order : Order, orderStatus: OrderStatus) = {
        tradeGateCallbacks.foreach(_.onOrderStatus(order, orderStatus))
    }

    private def chkOrderExecution(ord: Order): Option[Trade] = {
        checkOrderAndGetTradePrice(ord) match {
            case None => None
            case Some(price)=>{
                val trd = new Trade(ord.qty, price, ord.side, ord, dtGmt, security)
                onTrade(trd)
                Some(trd);
            }
        }
    }


    def onTrade(trd: Trade) = {
        trades += trd
        val posBefore = position_
        position_ = trd.adjustPositionByThisTrade(position_)
        trd.positionAfter = position

        if (math.signum(posBefore) != math.signum(position)) {
            if (position == 0) {
                lastPositionTrade = null
            }
            else {
                lastPositionTrade = trd
            }
        }
        fireTradeEvent(trd)
    }

    def checkOrderAndGetTradePrice(ord: Order): Option[Double] = {

        (ord.orderType, ord.side) match {
            case (OrderType.Market,Side.Buy) => Some(ask)
            case (OrderType.Market,Side.Sell) => Some(bid)

            case (OrderType.Stop,Side.Buy) if middlePrice > ord.price => Some(ask)
            case (OrderType.Stop,Side.Sell) if middlePrice < ord.price=> Some(bid)

            case (OrderType.Limit,Side.Buy) if ask < ord.price => Some(ord.price)
            case (OrderType.Limit,Side.Sell) if bid > ord.price=> Some(ord.price)
            case _ => None
        }

    }

    override def position: Int = position_
}
