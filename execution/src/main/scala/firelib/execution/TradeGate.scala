package firelib.execution

import firelib.common.TradeGateCallback
import firelib.common.threading.ThreadExecutor
import firelib.common._

trait TradeGate {

    def sendOrder(order: Order)

    def cancelOrder(orderId: String)

    def registerCallback(tgc: TradeGateCallback)

    def configure(config: Map[String, String], symbolMapping: Map[String, String], callbackExecutor: ThreadExecutor)

    def start()
}
