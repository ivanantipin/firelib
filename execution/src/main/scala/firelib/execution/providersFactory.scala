package firelib.execution

import firelib.common.threading.ThreadExecutor
import firelib.execution.config.ModelRuntimeConfig

object providersFactory {
    def create(cfg: ModelRuntimeConfig, executor: ThreadExecutor): (TradeGate, MarketDataProvider) = {
        if (cfg.gatewayType == "IB") {
            val gate = Class.forName("firelib.ibadapter.IbTradeGate").newInstance().asInstanceOf[TradeGate]
            val marketDataProvider = gate.asInstanceOf[MarketDataProvider]
            gate.configure(cfg.gatewayConfig, cfg.ibContractMapping, executor)
            gate.start()
            return (gate, marketDataProvider)
        }
        throw new Exception("not supported gateway type " + cfg.gatewayType)
    }
}
