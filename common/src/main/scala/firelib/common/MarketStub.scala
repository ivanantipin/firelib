package firelib.common

import java.time.Instant

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class MarketStub(val security: String, val maxOrderCount: Int = 20) extends IMarketStub {

    var bidAsk = Array(Double.NaN, Double.NaN)


    var dtGmt: Instant  =_

    val orders_ = new mutable.HashMap[String, Order]()

    def orders = orders_.values.toArray[Order]

    val tradeGateCallbacks = ArrayBuffer[ITradeGateCallback]()

    var position_ = 0
    val trades = new ListBuffer[Trade]()

    var orderIdCnt = 0
    var lastPositionTrade: Trade = _

    val hasPendingState = false


    private def middlePrice: Double = {
        bidAsk.sum / 2
    }


    def addCallback(callback: ITradeGateCallback) = tradeGateCallbacks += callback


    def moveCallbacksTo(marketStub: IMarketStub) = {
        tradeGateCallbacks.foreach(marketStub.addCallback)
        tradeGateCallbacks.clear()
    }

    def flattenAll(reason: String = null) = {
        cancelAllOrders()
        closePosition(reason)
    }

    def cancelAllOrders() = {
        val ords = orders_.clone()
        orders_.clear()
        fireOrderState(ords.values, OrderStatus.Cancelled)
    }

    def cancelOrderByIds(orderIds: Seq[String]) = {
        orderIds.foreach(orderId => {
            val ord = orders_.remove(orderId)
            assert(ord.isDefined,"no order with id " + orderId)
            fireOrderState(List[Order] {
                ord.get
            }, OrderStatus.Cancelled)
        })
    }

    private def fireTradeEvent(trade: Trade) = tradeGateCallbacks.foreach(tgc => tgc.onTrade(trade))

    def submitOrders(orders: Seq[Order]) : Unit = {

        assert(this.orders_.size + orders.length <= maxOrderCount, "max order count exceeded")

        orders.foreach(order => {
            order.id = nextOrderId
            order.placementTime = dtGmt
            order.security = security
            fireOrderState(List(order), OrderStatus.New)
            if (order.minutesToHold != -1) {
                order.validUntil = dtGmt.plusSeconds(order.minutesToHold*60)
            }
            this.orders_(order.id) = order
        })
        checkOrders()
    }

    private def nextOrderId : String = {
        orderIdCnt+= 1
        security + orderIdCnt
    }


    def closePosition(reason: String): Unit = {
        if (position_ == 0)
            return

        val ord = new Order(OrderType.Market, 0, math.abs(position_), Side.SideForAmt(-position_)) {
            security = security
        }
        val trd = new Trade(math.abs(position_), checkOrderAndGetTradePrice(ord), ord.side, ord, dtGmt,
            security) {
            reason = reason
        }
        onTrade(trd)

        assert(position_ == 0,"position must be 0 after flatten all!!!")
    }

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant) = {
        bidAsk(0) = bid
        bidAsk(1) = ask
        if (position_ != 0) {
            lastPositionTrade.onPrice(if (lastPositionTrade.side == Side.Buy) bid else ask)
        }
        this.dtGmt = dtGmt
        checkOrders()
    }

    def checkOrders() = {
        orders_ retain { (id, ord) => {
            if (chkOrderExecution(ord) != null) {
                fireOrderState(List(ord), OrderStatus.Done)
                false
            }
            else {
                assert(ord.orderType != OrderType.Market, "market order should cause position change!!")
                if (ord.validUntil != null && ord.validUntil.isBefore(dtGmt)) {
                    fireOrderState(List(ord), OrderStatus.Cancelled)
                    false
                }
                true
            }
        }
        }

    }

    def fireOrderState(orders: Iterable[Order], orderStatus: OrderStatus) = {
        tradeGateCallbacks.foreach(tgc=>{
            orders.foreach(tgc.onOrderStatus(_,orderStatus))
        })

    }

    def chkOrderExecution(ord: Order): Trade = {
        val price = checkOrderAndGetTradePrice(ord)
        if (price.isNaN)
            return null
        val trd = new Trade(ord.qty, price, ord.side, ord, dtGmt, security)
        onTrade(trd)
        return trd
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

    def checkOrderAndGetTradePrice(ord: Order): Double = {

        (ord.orderType, ord.side) match {
            case (OrderType.Market,Side.Buy) => return bidAsk(1)
            case (OrderType.Market,Side.Sell) => return bidAsk(0)

            case (OrderType.Stop,Side.Buy) if middlePrice > ord.price => return bidAsk(1)
            case (OrderType.Stop,Side.Sell) if middlePrice < ord.price=> return bidAsk(0)

            case (OrderType.Limit,Side.Buy) if bidAsk(1) < ord.price => return ord.price
            case (OrderType.Limit,Side.Sell) if bidAsk(0) > ord.price=> return ord.price
            case _ => return Double.NaN
        }

    }

    override def position: Int = position_
}
