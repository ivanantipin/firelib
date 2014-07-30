package firelib.common

/**
 * this is listener for market trade/order events
 */
trait ITradeGateCallback {
    def onTrade(trade: Trade)

    def onOrderStatus(order: Order, status: OrderStatus)
}
