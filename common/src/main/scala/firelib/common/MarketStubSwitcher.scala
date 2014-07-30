package firelib.common

import java.time.Instant

class MarketStubSwitcher(val first: IMarketStub, val secondary: IMarketStub) extends IMarketStub {

    private var activeStub = first

    def Position = activeStub.Position

    def hasPendingState = activeStub.hasPendingState

    val Security = activeStub.Security

    def submitOrders(orders: Seq[Order]) = activeStub.submitOrders(orders)

    def switchStubs() = {
        if (activeStub == first) {
            first.removeCallbacksTo(secondary);
            activeStub = secondary;
        }
        else {
            secondary.removeCallbacksTo(first);
            activeStub = first;
        }
    }

    def flattenAll(reason: String) = activeStub.flattenAll(reason)

    def trades = activeStub.trades

    def cancelOrders = activeStub.cancelOrders

    def orders = activeStub.orders

    def cancelOrderByIds(orderIds: Seq[String]) = activeStub.cancelOrderByIds(orderIds)

    def addCallback(callback: ITradeGateCallback) = activeStub.addCallback(callback)

    def removeCallbacksTo(marketStub: IMarketStub) = activeStub.removeCallbacksTo(marketStub)

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant) = activeStub.updateBidAskAndTime(bid, ask, dtGmt);
}
