package firelib.common

import java.time.Instant

class MarketStubSwitcher(val first: IMarketStub, val secondary: IMarketStub) extends IMarketStub {

    private var activeStub = first

    def Position = activeStub.Position

    def HasPendingState = activeStub.HasPendingState

    val Security = activeStub.Security

    def SubmitOrders(orders: Seq[Order]) = activeStub.SubmitOrders(orders)

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

    def CancelOrderByIds(orderIds: Seq[String]) = activeStub.CancelOrderByIds(orderIds)

    def AddCallback(callback: ITradeGateCallback) = activeStub.AddCallback(callback)

    def RemoveCallbacksTo(marketStub: IMarketStub) = activeStub.RemoveCallbacksTo(marketStub)

    def UpdateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant) = activeStub.UpdateBidAskAndTime(bid, ask, dtGmt);
}
