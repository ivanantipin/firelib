package firelib.execution

import firelib.common.threading.ThreadExecutor
import firelib.execution.config.ModelRuntimeConfig

object providersFactory {
    def create(cfg: ModelRuntimeConfig, executor: ThreadExecutor): (TradeGate, MarketDataProvider) = {
        val gate = Class.forName(cfg.tradeGateConfig.gateClassName).newInstance().asInstanceOf[TradeGate]
        val marketDataProvider =  gate.asInstanceOf[MarketDataProvider]
        gate.configure(cfg.tradeGateConfig.gateParams, executor)
        gate.start()
        return (gate, marketDataProvider)
    }
}
