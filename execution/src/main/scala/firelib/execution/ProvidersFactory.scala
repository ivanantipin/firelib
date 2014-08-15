package firelib.robot

import firelib.common.IThreadExecutor

class ProvidersFactory {
    def create(cfg: ModelRuntimeConfig, executor: IThreadExecutor): (ITradeGate, IMarketDataProvider) = {
        if (cfg.gatewayType == "IB") {
            val gate = Class.forName("firelib.ibadapter.IbTradeGate").newInstance().asInstanceOf[ITradeGate]
            val marketDataProvider = gate.asInstanceOf[IMarketDataProvider]
            gate.configure(cfg.gatewayConfig, cfg.ibContractMapping, executor)
            gate.start()
            return (gate, marketDataProvider)
        }
        throw new Exception("not supported gateway type " + cfg.gatewayType)
    }
}
