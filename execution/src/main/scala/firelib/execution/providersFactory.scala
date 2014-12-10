package firelib.execution

import firelib.common.marketstub.TradeGate
import firelib.common.threading.ThreadExecutor
import firelib.execution.config.ModelExecutionConfig

object providersFactory {
    def create(cfg: ModelExecutionConfig, executor: ThreadExecutor): (TradeGate, MarketDataProvider) = {
        val gate = Class.forName(cfg.tradeGateConfig.gateClassName).newInstance().asInstanceOf[TradeGate with Configurable]
        val marketDataProvider =  gate.asInstanceOf[MarketDataProvider]
        gate.start(cfg.tradeGateConfig.gateParams, executor)
        return (gate, marketDataProvider)
    }
}
