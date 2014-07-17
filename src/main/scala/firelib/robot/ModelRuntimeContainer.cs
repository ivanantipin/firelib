using System;
using System.IO;
using System.Linq;
using Fire.Common.Backtest;
using Fire.Common.Domain;
using Fire.Common.Threading;
using Fire.Common.Writers;
using NLog;
using QuantLib.Common;
using QuantLib.Domain;
using QuantLib.IndicatorBase;

namespace Fire.Common.Robot
{
    public class ModelRuntimeContainer
    {
        private readonly ModelRuntimeConfig modelRuntimeConfig;
        private readonly ThreadExecutor executor;
        private readonly IMarketDataProvider marketDataProvider;
        private readonly ITradeGate tradeGate;
        private readonly IModel model;
        private readonly MarketDataPlayer marketDataPlayer;
        private readonly Frequencer frequencer;
        private readonly IQuoteListener[] marketDataListeners;
        private readonly IStepListener[] stepListeners;
        private readonly RuntimeTradeWriter runtimeTradeWriter;

        private readonly Logger log;

        public ModelRuntimeContainer(ModelRuntimeConfig modelRuntimeConfig)
        {
            var threadName = modelRuntimeConfig.ModelConfig.ClassName;
           
            log = LogManager.GetLogger(threadName);

            log.Info("Starting config " + JsonSerializerUtil.ToStr(modelRuntimeConfig));
            this.modelRuntimeConfig = modelRuntimeConfig;

            runtimeTradeWriter = new RuntimeTradeWriter(Path.Combine(modelRuntimeConfig.TradeLogDirectory ?? Directory.GetCurrentDirectory(), "trades_live.csv"));

            executor = new ThreadExecutor(threadName: threadName);
            executor.Start();

            new ProvidersFactory().Create(modelRuntimeConfig, executor, out tradeGate, out marketDataProvider);

            Func<string, IMarketStub> marketStubFactory = (ti) => new MarketStubSwitcher(new MarketStub(ti), new ExecutionMarketStub(tradeGate, ti));

            var starter = new BacktesterSimple(marketStubFactory);

            MarketDataDistributor container;

            starter.RunSimple(modelRuntimeConfig.ModelConfig, out model, out marketDataPlayer, out container, modelRuntimeConfig.RunBacktest);
            marketDataListeners = marketDataPlayer.GetMarketDataListeners();
            stepListeners = marketDataPlayer.GetStepListeners();
            marketDataPlayer.Dispose();
            container.SwitchTickerType(TickerType.Tick);

            frequencer = new Frequencer(modelRuntimeConfig.ModelConfig.GetInterval());

            var stubSwitchers = model.GetStubs().Select(ms => (MarketStubSwitcher) ms);
            foreach (var switcher in stubSwitchers)
            {
                switcher.SwitchStubs();
                //to write trades to csv file
                switcher.AddCallback(new TradeGateCallbackAdapter((t) => runtimeTradeWriter.Write(modelRuntimeConfig.ModelConfig.ClassName, t)));
            }
            
            log.Info("Started ");
        }

        public void Start()
        {
            var interval = modelRuntimeConfig.ModelConfig.GetInterval();
            frequencer.AddListener((dt) => executor.Execute(() =>
                                                                {
                                                                    var roundTime = interval.RoundTime(DateTime.Now);
                                                                    foreach (var stepListener in stepListeners)
                                                                    {
                                                                        stepListener.OnStep(roundTime);
                                                                    }
                                                                }));


            foreach (var marketDataListener in marketDataListeners)
            {
                for (int k = 0; k < modelRuntimeConfig.ModelConfig.TickerIds.Count; k++)
                {
                    int k1 = k;
                    IQuoteListener listener = marketDataListener;
                    //!!!! assumed that market data provider works in the same thread
                    marketDataProvider.Subscribe(modelRuntimeConfig.ModelConfig.TickerIds[k].TickerId, (q) => listener.AddQuote(k1, q));
                }
            }


            frequencer.Start();
        }
    }
}