package firelib.common

/**
 * this is listener for market trade/order events
 */
trait ITradeGateCallback {
    def OnTrade(trade: Trade)

    def OnOrderStatus(order: Order, status: OrderStatus)
}
