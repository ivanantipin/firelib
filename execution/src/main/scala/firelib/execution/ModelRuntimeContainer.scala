package firelib.robot

import java.nio.file.Paths
import java.time.Instant

import firelib.backtest._
import firelib.common._
import firelib.utils.RuntimeTradeWriter
import org.slf4j.LoggerFactory
class ModelRuntimeContainer(val modelRuntimeConfig: ModelRuntimeConfig) {

    private var frequencer: Frequencer=_

    val log = LoggerFactory.getLogger(getClass)


    //FIXME log.info("Starting config " + JsonSerializerUtil.ToStr(modelRuntimeConfig));


    val tradesPath = Paths.get(modelRuntimeConfig.tradeLogDirectory.getOrElse("trades_live.csv")).toFile.getName


    val executor = new ThreadExecutor(threadName = modelRuntimeConfig.modelConfig.ClassName).Start();


    val (tradeGate, marketDataProvider) = new ProvidersFactory().Create(modelRuntimeConfig, executor);

    val marketStubFactory = (ti: String) => new MarketStubSwitcher(new MarketStub(ti), new ExecutionMarketStub(tradeGate, ti));

    val starter = new BacktesterSimple(marketStubFactory);

    //out
    val (model, marketDataPlayer, distributor) = starter.RunSimple(modelRuntimeConfig.modelConfig, modelRuntimeConfig.RunBacktest)

    private val stepListeners = marketDataPlayer.getStepListeners();

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
            val now = Instant.ofEpochMilli(interval.roundEpochMs(System.currentTimeMillis()))
            val act = () => stepListeners.foreach(_.OnStep(now))
            executor.Execute(()=>act())
        });


        for (k <- 0 until modelRuntimeConfig.modelConfig.TickerIds.length) {
            val k1 = k;
            //!!!! assumed that market data provider works in the same thread
            marketDataProvider.SubscribeForTick(modelRuntimeConfig.modelConfig.TickerIds(k).TickerId, (q: Tick) => distributor.onTick(k1, q, null));
        }

        frequencer.Start();
    }
}
