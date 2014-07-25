package firelib.robot

import firelib.backtest.IMarketStub
import firelib.domain._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer


class ExecutionMarketStub(val tradeGate: ITradeGate, val security: String, val maxOrderCount: Int = 20) extends IMarketStub with ITradeGateCallback {


    tradeGate.RegisterCallback(this);

    private val bidAsk = Array(Double.NaN, Double.NaN);

    private var dtGmt: DateTime =_

    private val orders_ = new ArrayBuffer[Order]();

    private val tradeGateCallbacks = new ArrayBuffer[ITradeGateCallback]();

    private var position = 0

    private val trades_ = new ArrayBuffer[Trade]();

    private var orderIdCnt = 0;

    val log = LoggerFactory.getLogger(getClass)


    def trades: Iterable[Trade] = trades_

    def orders: Iterable[Order] = orders_

    /*public int UnconfirmedPosition {
        get
        {
            var marketOrderPositioins = orders.Sum(o => o.OrderType == OrderType.Market ? o.Qty*o.Side.SignForSide() : 0);
            return position + marketOrderPositioins;
        }
    }*/

    def HasPendingState: Boolean = {
        orders_.exists(o => (o.Status.IsPending || (o.OrdType == OrderType.Market)))
    }


    def AddCallback(callback: ITradeGateCallback) = {
        tradeGateCallbacks += callback
    }

    def RemoveCallbacksTo(marketStub: IMarketStub) = {
        tradeGateCallbacks.foreach(marketStub.AddCallback);
        tradeGateCallbacks.clear();
    }

    def FlattenAll(reason: String = null) = {
        CancelOrders();
        ClosePosition(reason);
    }


    def CancelOrders() : Unit = {
        CancelOrderByIds(orders_.map(o => o.Id))
    }

    def CancelOrderByIds(orderIds: Seq[String]) : Unit = {

        for (orderId <- orderIds) {
            val ordOp = orders_.find(o => o.Id == orderId);
            if (ordOp.isDefined) {
                val ord = ordOp.get
                tradeGate.CancelOrder(orderId);
                ord.Status = OrderStatus.PendingCancel;
                tradeGateCallbacks.foreach(tgc => tgc.OnOrderStatus(ord, OrderStatus.PendingCancel))
            }else {
                log.error("cancelling non existing order " + orderId);
            }
        }
    }


    def SubmitOrders(orders: Seq[Order]) = {
        if (this.orders_.length > maxOrderCount) {
            throw new Exception("max order count exceeded");
        }

        orders.foreach(order => {
            order.Id = Security + "" + (orderIdCnt += 1);
            order.PlacementTime = dtGmt;
            order.Security = Security;
            FireOrderState(List(order), OrderStatus.New);
            if (order.MinutesToHold != -1) {
                order.ValidUntil = dtGmt.plusMinutes(order.MinutesToHold);
            }
            this.orders_ += order;
            log.info("submitting order " + order);
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
        val order: Order = new Order(OrderType.Market, 0, math.abs(position), Side.SideForAmt(-position)) {
            Security = security
        }
        SubmitOrders(List(order));
    }

    def UpdateBidAskAndTime(bid: Double, ask: Double, dtGmt: DateTime) {
        bidAsk(0) = bid;
        bidAsk(1) = ask;
        this.dtGmt = dtGmt;
    }


    def OnOrderStatus(order: Order, status: OrderStatus): Unit = {
        if (!orders_.exists(o => o.Id == order.Id)) {
            return;
        }
        log.info(String.format("order status %s status %s ", order , status));

        if (status.IsFinal) {
            orders_.remove(orders_.indexWhere(o => o.Id == order.Id))
        }
        tradeGateCallbacks.foreach(tg => tg.OnOrderStatus(order, status))
    }

    def OnTrade(trd: Trade): Unit = {
        if (!orders_.exists(o => o.Id == trd.SrcOrder.Id)) {
            return;
        }
        log.info(String.format("on trade %s ", trd));
        trades_ += trd
        position = trd.AdjustPositionByThisTrade(position);
        trd.PositionAfter = Position;
        log.info(String.format("position after %s ", Position));
        tradeGateCallbacks.foreach(tgc => tgc.OnTrade(trd));
    }

    override val Position: Int = position
    override val Security: String = security
}
