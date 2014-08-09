package firelib.robot

import firelib.common.IThreadExecutor

class ProvidersFactory {
    def Create(cfg: ModelRuntimeConfig, executor: IThreadExecutor): (ITradeGate, IMarketDataProvider) = {
        if (cfg.GatewayType == "IB") {
            val gate = Class.forName("IbTradeGate").newInstance().asInstanceOf[ITradeGate]
            val marketDataProvider = gate.asInstanceOf[IMarketDataProvider]
            gate.configure(cfg.gatewayConfig, cfg.IbContractMapping, executor)
            gate.start()
            return (gate, marketDataProvider)
        }
        throw new Exception("not supported gateway type " + cfg.GatewayType)
    }
}
