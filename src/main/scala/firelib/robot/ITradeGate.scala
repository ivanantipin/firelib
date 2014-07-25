package firelib.robot

import firelib.backtest.IThreadExecutor
import firelib.domain.Order

trait ITradeGate {
    def SendOrder(order: Order)

    def CancelOrder(orderId: String)

    def RegisterCallback(tgc: ITradeGateCallback)

    def Configure(config: Map[String, String], symbolMapping: Map[String, String], callbackExecutor: IThreadExecutor)

    def Start();
}
