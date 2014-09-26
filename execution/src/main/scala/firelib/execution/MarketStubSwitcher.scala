package firelib.execution

import java.time.Instant

import firelib.common.{Order, TradeGateCallback}
import firelib.common.marketstub.MarketStub

class MarketStubSwitcher(val first: MarketStub, val secondary: MarketStub) extends MarketStub {

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

    def addCallback(callback: TradeGateCallback) = activeStub.addCallback(callback)

    def moveCallbacksTo(marketStub: MarketStub) = activeStub.moveCallbacksTo(marketStub)

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant) = activeStub.updateBidAskAndTime(bid, ask, dtGmt)

    override def nextOrderId: String = activeStub.nextOrderId
}
