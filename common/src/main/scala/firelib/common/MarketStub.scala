package firelib.common

import java.time.Instant

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class MarketStub(val Security: String, val maxOrderCount: Int = 20) extends IMarketStub {

    var bidAsk = Array(Double.NaN, Double.NaN)


    var dtGmt: Instant  =_

    val orders_ = new mutable.HashMap[String, Order]()

    def orders = orders_.values.toArray[Order]

    val tradeGateCallbacks = ArrayBuffer[ITradeGateCallback]()

    var position = 0;
    val trades = new ListBuffer[Trade]()

    var orderIdCnt = 0
    var lastPositionTrade: Trade = _

    val hasPendingState = false


    private def middlePrice: Double = {
        bidAsk.sum / 2;
    }


    def addCallback(callback: ITradeGateCallback) = tradeGateCallbacks += callback


    def removeCallbacksTo(marketStub: IMarketStub) = {
        tradeGateCallbacks.foreach(marketStub.addCallback);
        tradeGateCallbacks.clear()
    }

    def flattenAll(reason: String = null) = {
        cancelOrders();
        closePosition(reason);
    }

    def cancelOrders() = {
        val ords = orders_.clone()
        orders_.clear();
        fireOrderState(ords.values, OrderStatus.Cancelled);
    }

    def cancelOrderByIds(orderIds: Seq[String]) = {
        orderIds.foreach(orderId => {
            val ord = orders_.remove(orderId)
            assert(ord.isDefined,"no order with id " + orderId)
            fireOrderState(List[Order] {
                ord.get
            }, OrderStatus.Cancelled);
        });
    }

    private def fireTradeEvent(trade: Trade) = tradeGateCallbacks.foreach(tgc => tgc.onTrade(trade))

    def submitOrders(orders: Seq[Order]) : Unit = {

        assert(this.orders_.size + orders.length <= maxOrderCount, "max order count exceeded")

        orders.foreach(order => {
            order.Id = nextOrderId
            order.PlacementTime = dtGmt;
            order.Security = Security;
            fireOrderState(List(order), OrderStatus.New);
            if (order.MinutesToHold != -1) {
                order.ValidUntil = dtGmt.plusSeconds(order.MinutesToHold*60);
            }
            this.orders_(order.Id) = order
        })
        checkOrders()
    }

    private def nextOrderId : String = {
        orderIdCnt+= 1
        Security + orderIdCnt
    }


    def closePosition(reason: String): Unit = {
        if (position == 0)
            return;

        val ord = new Order(OrderType.Market, 0, math.abs(position), Side.SideForAmt(-position)) {
            Security = Security
        };
        val trd = new Trade(math.abs(position), checkOrderAndGetTradePrice(ord), ord.OrderSide, ord, dtGmt,
            Security) {
            reason = reason
        };
        onTrade(trd);

        assert(position == 0,"position must be 0 after flatten all!!!")
    }

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant) = {
        bidAsk(0) = bid
        bidAsk(1) = ask
        if (position != 0) {
            lastPositionTrade.OnPrice(if (lastPositionTrade.TradeSide == Side.Buy) bid else ask);
        }
        this.dtGmt = dtGmt
        checkOrders()
    }

    def checkOrders() = {

        orders_ retain { (id, ord) => {
            if (chkOrderExecution(ord) != null) {
                fireOrderState(List(ord), OrderStatus.Done);
                false
            }
            else {
                assert(ord.OrdType != OrderType.Market, "market order should cause position change!!")
                if (ord.ValidUntil != null && ord.ValidUntil.isBefore(dtGmt)) {
                    fireOrderState(List(ord), OrderStatus.Cancelled);
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
        var price = checkOrderAndGetTradePrice(ord)
        if (price.isNaN)
            return null
        val trd = new Trade(ord.Qty, price, ord.OrderSide, ord, dtGmt, Security)
        onTrade(trd)
        return trd
    }


    def onTrade(trd: Trade) = {
        trades += trd
        val posBefore = position
        position = trd.AdjustPositionByThisTrade(position)
        trd.PositionAfter = Position

        if (math.signum(posBefore) != math.signum(Position)) {
            if (Position == 0) {
                lastPositionTrade = null
            }
            else {
                lastPositionTrade = trd
            }
        }
        fireTradeEvent(trd)
    }

    def checkOrderAndGetTradePrice(ord: Order): Double = {

        (ord.OrdType, ord.OrderSide) match {
            case (OrderType.Market,Side.Buy) => return bidAsk(1)
            case (OrderType.Market,Side.Sell) => return bidAsk(0)

            case (OrderType.Stop,Side.Buy) if middlePrice > ord.Price => return bidAsk(1)
            case (OrderType.Stop,Side.Sell) if middlePrice < ord.Price=> return bidAsk(0)

            case (OrderType.Limit,Side.Buy) if bidAsk(1) < ord.Price => return ord.Price
            case (OrderType.Limit,Side.Sell) if bidAsk(0) > ord.Price=> return ord.Price
            case _ => return Double.NaN
        }

    }

    override def Position: Int = position
}
