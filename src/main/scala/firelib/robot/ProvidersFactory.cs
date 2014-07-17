using System;
using Fire.Common.Threading;
using QuantLib.Common;

namespace Fire.Common.Robot
{
    public class ProvidersFactory
    {
        public void Create(ModelRuntimeConfig cfg, IThreadExecutor executor,  out ITradeGate tradeGate, out IMarketDataProvider marketDataProvider)
        {
            if (cfg.GatewayType == "IB")
            {
                tradeGate = ReflectionCreator<ITradeGate>.Create("IbTradeGate");
                marketDataProvider = (IMarketDataProvider) tradeGate;
                tradeGate.Configure(cfg.GatewayConfig, cfg.IbContractMapping, executor);
                tradeGate.Start();
                return;
            }
            throw new Exception("not supported gateway type " + cfg.GatewayType);
        }
    }
}