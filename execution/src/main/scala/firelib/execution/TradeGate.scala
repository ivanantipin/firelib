package firelib.execution

import firelib.common.threading.ThreadExecutor
import firelib.common.{TradeGateCallback, _}

/**
 * Main interface for execution to adapt broker api.
 */
trait TradeGate {

    /**
     * just order send
     */
    def sendOrder(order: Order)

    /**
     * just order cancel by id
     */
    def cancelOrder(orderId: String)

    /**
     * register callback to receive notifications about trades and order statuses
     */
    def registerCallback(tgc: TradeGateCallback)

    /**
     * pass configuration params to gate
     * usually it is user/password, broker port and url etc
     */
    def configure(config: Map[String, String], callbackExecutor: ThreadExecutor)

    /**
     * need to run start to finish initialization
     */
    def start()
}
