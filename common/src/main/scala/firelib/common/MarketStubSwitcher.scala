package firelib.common

import java.time.Instant

class MarketStubSwitcher(val first: IMarketStub, val secondary: IMarketStub) extends IMarketStub {

    private var activeStub = first

    def position = activeStub.position

    def hasPendingState = activeStub.hasPendingState

    val security = activeStub.security

    def submitOrders(orders: Seq[Order]) = activeStub.submitOrders(orders)

    def switchStubs() = {
        if (activeStub == first) {
            first.moveCallbacksTo(secondary)
            activeStub = secondary        }
        else {
            secondary.moveCallbacksTo(first)
            activeStub = first
        }
    }

    def flattenAll(reason: Option[String]) = activeStub.flattenAll(reason)

    def trades = activeStub.trades

    def cancelAllOrders = activeStub.cancelAllOrders

    def orders = activeStub.orders

    def cancelOrderByIds(orderIds: Seq[String]) = activeStub.cancelOrderByIds(orderIds)

    def addCallback(callback: ITradeGateCallback) = activeStub.addCallback(callback)

    def moveCallbacksTo(marketStub: IMarketStub) = activeStub.moveCallbacksTo(marketStub)

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant) = activeStub.updateBidAskAndTime(bid, ask, dtGmt)
}
