package firelib.backtest

import firelib.domain.Order
import firelib.robot.ITradeGateCallback
import org.joda.time.DateTime

class MarketStubSwitcher(val first: IMarketStub, val secondary: IMarketStub) extends IMarketStub {

    private var activeStub = first

    def Position = activeStub.Position

    def UnconfirmedPosition = activeStub.UnconfirmedPosition

    def HasPendingState = activeStub.HasPendingState

    def Security = activeStub.Security

    def SubmitOrders(orders: Order*) = activeStub.SubmitOrders(orders)

    def SwitchStubs() = {
        if (activeStub == first) {
            first.RemoveCallbacksTo(secondary);
            activeStub = secondary;
        }
        else {
            secondary.RemoveCallbacksTo(first);
            activeStub = first;
        }
    }

    def FlattenAll(reason: String) = activeStub.FlattenAll(reason)

    def trades = activeStub.trades

    def CancelOrders = activeStub.CancelOrders

    def orders = activeStub.orders

    def CancelOrderByIds(orderIds: Array[String]) = activeStub.CancelOrderByIds(orderIds)

    def AddCallback(callback: ITradeGateCallback) = activeStub.AddCallback(callback)

    def RemoveCallbacksTo(marketStub: IMarketStub) = activeStub.RemoveCallbacksTo(marketStub)

    def UpdateBidAskAndTime(bid: Double, ask: Double, dtGmt: DateTime) = activeStub.UpdateBidAskAndTime(bid, ask, dtGmt);
}
