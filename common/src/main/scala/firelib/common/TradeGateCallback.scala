package firelib.common

import firelib.common.{Order, OrderStatus, Trade}

/**
 * this is listener for market trade/order events
 */
trait TradeGateCallback {
    def onTrade(trade: Trade)

    def onOrderStatus(order: Order, status: OrderStatus)
}
