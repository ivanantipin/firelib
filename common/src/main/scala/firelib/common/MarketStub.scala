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

    val HasPendingState = false


    private def Middle: Double = {
        bidAsk.sum / 2;
    }


    def AddCallback(callback: ITradeGateCallback) = tradeGateCallbacks += callback


    def RemoveCallbacksTo(marketStub: IMarketStub) = {
        tradeGateCallbacks.foreach(marketStub.AddCallback);
        tradeGateCallbacks.clear()
    }

    def FlattenAll(reason: String = null) = {
        CancelOrders();
        ClosePosition(reason);
    }

    def CancelOrders() = {
        val ords = orders_.clone()
        orders_.clear();
        FireOrderState(ords.values, OrderStatus.Cancelled);
    }

    def CancelOrderByIds(orderIds: Seq[String]) = {

        orderIds.foreach(orderId => {
            var ord = orders_.remove(orderId)

            assert(ord.isDefined,"no order with id " + orderId)

            FireOrderState(List[Order] {
                ord.get
            }, OrderStatus.Cancelled);
        });
    }

    def FireTradeEvent(trade: Trade) = {
        tradeGateCallbacks.foreach(tgc => tgc.OnTrade(trade));
    }

    def SubmitOrders(orders: Seq[Order]) : Unit = {

        assert(this.orders_.size + orders.length <= maxOrderCount, "max order count exceeded")

        orders.foreach(order => {
            order.Id = nextOrderId
            order.PlacementTime = dtGmt;
            order.Security = Security;
            FireOrderState(List(order), OrderStatus.New);
            if (order.MinutesToHold != -1) {
                order.ValidUntil = dtGmt.plusSeconds(order.MinutesToHold*60);
            }
            this.orders_(order.Id) = order
        })
        CheckOrders()
    }

    private def nextOrderId : String = {
        orderIdCnt+= 1
        Security + orderIdCnt
    }


    def ClosePosition(reason: String): Unit = {
        if (position == 0)
            return;

        val ord = new Order(OrderType.Market, 0, math.abs(position), Side.SideForAmt(-position)) {
            Security = Security
        };
        val trd = new Trade(math.abs(position), CheckOrderAndGetTradePrice(ord), ord.OrderSide, ord, dtGmt,
            Security) {
            reason = reason
        };
        OnTrade(trd);

        assert(position == 0,"position must be 0 after flatten all!!!")
    }

    def UpdateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant) = {
        bidAsk(0) = bid;
        bidAsk(1) = ask;
        if (position != 0) {
            lastPositionTrade.OnPrice(if (lastPositionTrade.TradeSide == Side.Buy) bid else ask);
        }
        this.dtGmt = dtGmt;
        CheckOrders;
    }

    def CheckOrders() = {

        orders_ retain { (id, ord) => {
            if (ChkOrderExecution(ord) != null) {
                FireOrderState(List(ord), OrderStatus.Done);
                false
            }
            else {
                assert(ord.OrdType != OrderType.Market, "market order should cause position change!!")
                if (ord.ValidUntil != null && ord.ValidUntil.isBefore(dtGmt)) {
                    FireOrderState(List(ord), OrderStatus.Cancelled);
                    false
                }
                true
            }
        }
        }

    }

    def FireOrderState(orders: Iterable[Order], orderStatus: OrderStatus) = {
        tradeGateCallbacks.foreach(tgc=>{
            orders.foreach(tgc.OnOrderStatus(_,orderStatus))
        })

    }

    def ChkOrderExecution(ord: Order): Trade = {
        var price = CheckOrderAndGetTradePrice(ord);
        if (price.isNaN)
            return null;
        var trd = new Trade(ord.Qty, price, ord.OrderSide, ord, dtGmt, Security);
        OnTrade(trd);
        return trd;
    }


    def OnTrade(trd: Trade) = {
        trades += trd;
        val posBefore = position;
        position = trd.AdjustPositionByThisTrade(position);
        trd.PositionAfter = Position;

        if (math.signum(posBefore) != math.signum(Position)) {
            if (Position == 0) {
                lastPositionTrade = null;
            }
            else {
                lastPositionTrade = trd;
            }
        }
        FireTradeEvent(trd);
    }

    def CheckOrderAndGetTradePrice(ord: Order): Double = {

        (ord.OrdType, ord.OrderSide) match {
            case (OrderType.Market,Side.Buy) => return bidAsk(1)
            case (OrderType.Market,Side.Sell) => return bidAsk(0)

            case (OrderType.Stop,Side.Buy) if Middle > ord.Price => return bidAsk(1)
            case (OrderType.Stop,Side.Sell) if Middle < ord.Price=> return bidAsk(0)

            case (OrderType.Limit,Side.Buy) if bidAsk(1) < ord.Price => return ord.Price
            case (OrderType.Limit,Side.Sell) if bidAsk(0) > ord.Price=> return ord.Price
            case _ => return Double.NaN
        }

    }

    override def Position: Int = position
}
