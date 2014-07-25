package firelib.backtest

import firelib.domain._
import firelib.robot.ITradeGateCallback
import org.joda.time.DateTime

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class MarketStub(val Security: String, val maxOrderCount: Int = 20) extends IMarketStub {

    var bidAsk = Array(Double.NaN, Double.NaN)


    var dtGmt: DateTime =_

    val orders = new mutable.HashMap[String, Order]()

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
        val ords = orders.clone()
        orders.clear();
        FireOrderState(ords.values, OrderStatus.Cancelled);
    }

    def CancelOrderByIds(orderIds: Seq[String]) = {

        orderIds.foreach(orderId => {
            var ord = orders.remove(orderId)

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

        assert(this.orders.size <= maxOrderCount, "max order count exceeded")

        orders.foreach(order => {
            order.Id = Security + "" + (orderIdCnt += 1);
            order.PlacementTime = dtGmt;
            order.Security = Security;
            FireOrderState(List(order), OrderStatus.New);
            if (order.MinutesToHold != -1) {
                order.ValidUntil = dtGmt.plusMinutes(order.MinutesToHold);
            }
            this.orders(order.Id) = order
        })
        CheckOrders()
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

    def UpdateBidAskAndTime(bid: Double, ask: Double, dtGmt: DateTime) = {
        bidAsk(0) = bid;
        bidAsk(1) = ask;
        if (position != 0) {
            lastPositionTrade.OnPrice(if (lastPositionTrade.TradeSide == Side.Buy) bid else ask);
        }
        this.dtGmt = dtGmt;
        CheckOrders;
    }

    def CheckOrders() = {

        orders retain { (id, ord) => {
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

    def FireOrderState(tbro: Iterable[Order], orderStatus: OrderStatus) = {
        tbro.foreach(o => tradeGateCallbacks.foreach(tgc => tgc.OnOrderStatus(o, orderStatus)));
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
        if (ord.OrdType == OrderType.Market)
            return if (ord.OrderSide == Side.Buy) bidAsk(1) else bidAsk(0);

        if (ord.OrdType == OrderType.Stop) {
            if (ord.OrderSide == Side.Buy && Middle > ord.Price) {
                return bidAsk(1);
            }
            if (ord.OrderSide == Side.Sell && Middle < ord.Price) {
                return bidAsk(0);
            }
        }
        if (ord.OrdType == OrderType.Limit) {
            if (ord.OrderSide == Side.Buy && bidAsk(1) < ord.Price ||
              ord.OrderSide == Side.Sell && bidAsk(0) > ord.Price) {
                return ord.Price;
            }
        }
        Double.NaN
    }

    override def Position: Int = position
}
