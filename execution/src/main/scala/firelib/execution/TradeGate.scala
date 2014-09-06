package firelib.execution

import firelib.common.{TradeGateCallback, _}
import firelib.common.threading.ThreadExecutor

trait TradeGate {

    def sendOrder(order: Order)

    def cancelOrder(orderId: String)

    def registerCallback(tgc: TradeGateCallback)

    def configure(config: Map[String, String], callbackExecutor: ThreadExecutor)

    def start()
}
