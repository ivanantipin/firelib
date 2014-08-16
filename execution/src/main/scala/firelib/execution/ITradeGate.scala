package firelib.execution

import firelib.common._

trait ITradeGate {
    def sendOrder(order: Order)

    def cancelOrder(orderId: String)

    def registerCallback(tgc: ITradeGateCallback)

    def configure(config: Map[String, String], symbolMapping: Map[String, String], callbackExecutor: IThreadExecutor)

    def start()
}
