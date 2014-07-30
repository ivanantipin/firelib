package firelib.robot

import java.time.Instant

import firelib.common._
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer


class ExecutionMarketStub(val tradeGate: ITradeGate, val security: String, val maxOrderCount: Int = 20) extends IMarketStub with ITradeGateCallback {


    tradeGate.registerCallback(this);

    private val bidAsk = Array(Double.NaN, Double.NaN);

    private var dtGmt: Instant =_

    private val orders_ = new ArrayBuffer[Order]();

    private val tradeGateCallbacks = new ArrayBuffer[ITradeGateCallback]();

    private var position = 0

    private val trades_ = new ArrayBuffer[Trade]();

    private var orderIdCnt = 0;

    private val log = LoggerFactory.getLogger(getClass)


    def trades: Seq[Trade] = trades_

    def orders: Seq[Order] = orders_

    /*public int UnconfirmedPosition {
        get
        {
            var marketOrderPositioins = orders.Sum(o => o.OrderType == OrderType.Market ? o.Qty*o.Side.SignForSide() : 0);
            return position + marketOrderPositioins;
        }
    }*/

    override def hasPendingState: Boolean = {
        orders_.exists(o => (o.Status.IsPending || (o.OrdType == OrderType.Market)))
    }


    def addCallback(callback: ITradeGateCallback) = {
        tradeGateCallbacks += callback
    }

    def removeCallbacksTo(marketStub: IMarketStub) = {
        tradeGateCallbacks.foreach(marketStub.addCallback);
        tradeGateCallbacks.clear();
    }

    def flattenAll(reason: String = null) = {
        cancelOrders();
        closePosition(reason);
    }


    def cancelOrders() : Unit = cancelOrderByIds(orders_.map(_.Id))


    def cancelOrderByIds(orderIds: Seq[String]) : Unit = {

        for (orderId <- orderIds) {
            orders_.find(_.Id == orderId) match {
                case Some(ord) => {
                    tradeGate.cancelOrder(orderId);
                    ord.Status = OrderStatus.PendingCancel;
                    tradeGateCallbacks.foreach(_.onOrderStatus(ord, OrderStatus.PendingCancel))

                }
                case None => log.error("cancelling non existing order " + orderId);
            }
        }
    }


    def submitOrders(orders: Seq[Order]) = {

        assert(this.orders_.length <= maxOrderCount,"max order count exceeded")

        orders.foreach(order => {
            order.Id = Security + "" + (orderIdCnt += 1);
            order.PlacementTime = dtGmt;
            order.Security = Security;
            fireOrderState(List(order), OrderStatus.New);
            if (order.MinutesToHold != -1) {
                order.ValidUntil = dtGmt.plusSeconds(order.MinutesToHold*60);
            }
            this.orders_ += order;
            log.info("submitting order " + order);
            tradeGate.sendOrder(order);
        })
    }

    def fireOrderState(list: Traversable[Order], orderStatus: OrderStatus) = {
        list.foreach(order => tradeGateCallbacks.foreach(tgc => tgc.onOrderStatus(order, orderStatus)))
    }


    def closePosition(reason: String = null): Unit = {
        if (position == 0)
            return;

        if (hasPendingState) {
            return;
        }
        val order: Order = new Order(OrderType.Market, 0, math.abs(position), Side.SideForAmt(-position)) {
            Security = security
        }
        submitOrders(List(order));
    }

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt: Instant) {
        bidAsk(0) = bid;
        bidAsk(1) = ask;
        this.dtGmt = dtGmt;
    }


    def onOrderStatus(order: Order, status: OrderStatus): Unit = {
        if (!orders_.exists(o => o.Id == order.Id)) {
            return;
        }
        log.info(String.format("order status %s status %s ", order , status));

        if (status.IsFinal) {
            orders_.remove(orders_.indexWhere(o => o.Id == order.Id))
        }
        tradeGateCallbacks.foreach(tg => tg.onOrderStatus(order, status))
    }

    def onTrade(trd: Trade): Unit = {
        if (!orders_.exists(_.Id == trd.SrcOrder.Id)) {
            return;
        }
        log.info(String.format("on trade %s ", trd));
        trades_ += trd
        position = trd.AdjustPositionByThisTrade(position);
        trd.PositionAfter = Position;
        log.info("position after %d ".format(Position));
        tradeGateCallbacks.foreach(tgc => tgc.onTrade(trd));
    }

    override val Position: Int = position
    override val Security: String = security
}
