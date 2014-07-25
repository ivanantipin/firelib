package firelib.robot

import java.nio.file.Paths

import firelib.backtest._
import firelib.domain.Tick
import firelib.util.RuntimeTradeWriter
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

class ModelRuntimeContainer(val modelRuntimeConfig: ModelRuntimeConfig) {
    var executor: ThreadExecutor;
    private var marketDataProvider: IMarketDataProvider
    private var tradeGate: ITradeGate
    private var model: IModel
    private var frequencer: Frequencer
    private val distributor: MarketDataDistributor

    private var stepListeners: Seq[IStepListener]


    val log = LoggerFactory.getLogger(getClass)


    log.info("Starting config " + JsonSerializerUtil.ToStr(modelRuntimeConfig));


    val tradesPath = Paths.get(modelRuntimeConfig.tradeLogDirectory.getOrElse("trades_live.csv")).toFile.getName


    executor = new ThreadExecutor(threadName = modelRuntimeConfig.modelConfig.ClassName);
    executor.Start();

    (tradeGate, marketDataProvider) = new ProvidersFactory().Create(modelRuntimeConfig, executor);

    val marketStubFactory = (ti: String) => new MarketStubSwitcher(new MarketStub(ti), new ExecutionMarketStub(tradeGate, ti));

    var starter = new BacktesterSimple(marketStubFactory);


    var marketDataPlayer: MarketDataPlayer
    //out
    (model, marketDataPlayer, distributor) = starter.RunSimple(modelRuntimeConfig.modelConfig, modelRuntimeConfig.RunBacktest)

    stepListeners = marketDataPlayer.getStepListeners();
    marketDataPlayer.Dispose();


    frequencer = new Frequencer(modelRuntimeConfig.modelConfig.interval);

    var stubSwitchers = model.stubs.map(ms => ms.asInstanceOf[MarketStubSwitcher])

    for (switcher <- stubSwitchers) {
        switcher.SwitchStubs();
        //to write trades to csv file
        switcher.AddCallback(new TradeGateCallbackAdapter((t) => RuntimeTradeWriter.write(tradesPath, modelRuntimeConfig.modelConfig.ClassName, t)));
    }

    log.info("Started ");


    def Start() = {
        var interval = modelRuntimeConfig.modelConfig.interval;

        frequencer.AddListener((dt) => {
            val act = () => stepListeners.foreach(_.OnStep(interval.RoundTime(DateTime.now())))
            executor.Execute(act)
        });


        for (k <- 0 until modelRuntimeConfig.modelConfig.TickerIds.length) {
            val k1 = k;
            //!!!! assumed that market data provider works in the same thread
            marketDataProvider.Subscribe(modelRuntimeConfig.modelConfig.TickerIds(k).TickerId, (q: Tick) => distributor.onTick(k1, q, null));
        }

        frequencer.Start();
    }
}
