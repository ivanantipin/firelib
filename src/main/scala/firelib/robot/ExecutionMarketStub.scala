package firelib.robot

import firelib.backtest.IMarketStub
import firelib.domain._
import org.joda.time.DateTime

import scala.collection.mutable.ArrayBuffer


class ExecutionMarketStub(val tradeGate: ITradeGate, val security: String, val maxOrderCount: Int = 20) extends IMarketStub with ITradeGateCallback {


    tradeGate.RegisterCallback(this);

    private val bidAsk = Array(Double.NaN, Double.NaN);

    private var dtGmt: DateTime

    private val orders = new ArrayBuffer[Order]();

    private val tradeGateCallbacks = new ArrayBuffer[ITradeGateCallback]();

    private var position = 0

    private val trades = new ArrayBuffer[Trade]();

    private var orderIdCnt = 0;

    //private Logger log = LogManager.GetLogger("market stub");


    def trades: Iterable[Trade] = trades

    def orders: Iterable[Order] = orders

    /*public int UnconfirmedPosition {
        get
        {
            var marketOrderPositioins = orders.Sum(o => o.OrderType == OrderType.Market ? o.Qty*o.Side.SignForSide() : 0);
            return position + marketOrderPositioins;
        }
    }*/

    def HasPendingState: Boolean = {
        orders.exists(o => (o.Status.IsPending || (o.OrdType == OrderTypeEnum.Market)))
    }


    def AddCallback(callback: ITradeGateCallback) = {
        tradeGateCallbacks += callback
    }

    def RemoveCallbacksTo(marketStub: IMarketStub) = {
        tradeGateCallbacks.foreach(marketStub.AddCallback);
        tradeGateCallbacks.clear();
    }

    def FlattenAll(reason: String = null) {
        CancelOrders();
        ClosePosition(reason);
    }

    def CancelOrders() {
        CancelOrderByIds(orders.map(o => o.Id));
    }

    def CancelOrderByIds(orderIds: Traversable[String]) = {

        for (orderId <- orderIds) {
            var ordOp = orders.find(o => o.Id == orderId);

            if (ordOp.isEmpty) {
                //log.Error("cancelling non existing order " + orderId);

                continue;
            }
            val ord = ordOp.get
            tradeGate.CancelOrder(orderId);
            ord.Status = OrderStatusEnum.PendingCancel;
            tradeGateCallbacks.foreach(tgc => tgc.OnOrderStatus(ord, OrderStatusEnum.PendingCancel))
        }
    }


    def SubmitOrders(orders: Iterable[Order]) = {
        if (this.orders.length > maxOrderCount) {
            throw new Exception("max order count exceeded");
        }

        orders.foreach(order => {
            order.Id = Security + "" + (orderIdCnt += 1);
            order.PlacementTime = dtGmt;
            order.Security = Security;
            FireOrderState(List(order), OrderStatusEnum.New);
            if (order.MinutesToHold != -1) {
                order.ValidUntil = dtGmt.plusMinutes(order.MinutesToHold);
            }
            this.orders += order;
            //log.Info("submitting order " + order);
            tradeGate.SendOrder(order);
        })
    }

    def FireOrderState(list: Traversable[Order], orderStatus: OrderStatus) = {
        list.foreach(order => tradeGateCallbacks.foreach(tgc => tgc.OnOrderStatus(order, orderStatus)))
    }


    def ClosePosition(reason: String = null): Unit = {
        if (position == 0)
            return;

        if (HasPendingState) {
            return;
        }
        SubmitOrders(new Order(OrderTypeEnum.Market, 0, math.abs(position), SideEnum.SideForAmt(-position)) {
            Security = security
        });
    }

    def UpdateBidAskAndTime(bid: Double, ask: Double, dtGmt: DateTime) {
        bidAsk(0) = bid;
        bidAsk(1) = ask;
        this.dtGmt = dtGmt;
    }


    def OnOrderStatus(order: Order, status: OrderStatus): Unit = {
        if (!orders.exists(o => o.Id == order.Id)) {
            return;
        }
        //log.Info(string.Format("order status {0} status {1} ", order , status));

        if (status.IsFinal) {
            orders.FILTER(o => o.Id != order.Id)

        }
        tradeGateCallbacks.foreach(tg => tg.OnOrderStatus(order, status))
    }

    def OnTrade(trd: Trade): Unit = {
        if (!orders.exists(o => o.Id == trd.SrcOrder.Id)) {
            return;
        }
        //log.Info(string.Format("on trade {0} ", trd));
        trades += trd
        position = trd.AdjustPositionByThisTrade(position);
        trd.PositionAfter = Position;
        //log.Info(string.Format("position after {0} ", Position));
        tradeGateCallbacks.foreach(tgc => tgc.OnTrade(trd));
    }
}
