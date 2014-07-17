package firelib.robot

import firelib.domain.{Order, OrderStatus, Trade}

/**
 * this is listener for market trade/order events
 */
trait ITradeGateCallback {
    def OnTrade(trade: Trade)

    def OnOrderStatus(order: Order, status: OrderStatus)
}
