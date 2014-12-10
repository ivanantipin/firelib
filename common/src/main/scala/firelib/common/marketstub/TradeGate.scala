package firelib.common.marketstub

import firelib.common.misc.Topic
import firelib.common.{Order, Trade}
import firelib.domain.OrderState


/**
 * Main interface for execution to adapt broker api.
 */
trait TradeGate {
    /**
     * just order send
     */
    def sendOrder(order: Order) : (Topic[Trade],Topic[OrderState])
    /**
     * order cancel
     */
    def cancelOrder(order: Order)
}