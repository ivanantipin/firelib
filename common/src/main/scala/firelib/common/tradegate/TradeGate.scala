package firelib.common.tradegate

import firelib.common.misc.SubTopic
import firelib.common.{Order, Trade}
import firelib.domain.OrderState


/**
 * Main interface for execution to adapt broker api.
 */
trait TradeGate {
    /**
     * just order send
     */
    def sendOrder(order: Order) : (SubTopic[Trade],SubTopic[OrderState])
    /**
     * order cancel
     */
    def cancelOrder(order: Order)
}