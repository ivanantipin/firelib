package firelib.common

/**
 * this is listener for market trade/order events
 */
trait TradeGateCallback {


    def onTrade(trade: Trade)

    def onOrderStatus(order: Order, status: OrderStatus)
}

trait DisposableSubscription{
    def unsubscribe() : Unit
}
