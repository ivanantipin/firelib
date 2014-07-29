package firelib.robot

import firelib.common.IThreadExecutor

class ProvidersFactory {
    def Create(cfg: ModelRuntimeConfig, executor: IThreadExecutor): (ITradeGate, IMarketDataProvider) = {
        /*
                    if (cfg.GatewayType == "IB")
                    {
                        tradeGate = ReflectionCreator<ITradeGate>.Create("IbTradeGate");
                        marketDataProvider = (IMarketDataProvider) tradeGate;
                        tradeGate.Configure(cfg.GatewayConfig, cfg.IbContractMapping, executor);
                        tradeGate.Start();
                        return;
                    }
        */
        throw new Exception("not supported gateway type " + cfg.GatewayType);
    }
}
