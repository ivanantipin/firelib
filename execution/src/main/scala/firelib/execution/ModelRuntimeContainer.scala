package firelib.execution

import java.nio.file.Paths

import firelib.backtest._
import firelib.common._
import firelib.domain.Tick
import firelib.report.RuntimeTradeWriter
import firelib.utils.JacksonWrapper
import org.slf4j.LoggerFactory
class ModelRuntimeContainer(val modelRuntimeConfig: ModelRuntimeConfig) {

    val log = LoggerFactory.getLogger(getClass)

    log.info("Starting config " + JacksonWrapper.toJsonString(modelRuntimeConfig))

    val tradesPath = Paths.get(modelRuntimeConfig.tradeLogDirectory.getOrElse("trades_live.csv")).toFile.getName

    val modelConfig: ModelConfig = modelRuntimeConfig.modelConfig

    val executor = new ThreadExecutor(threadName = modelRuntimeConfig.modelConfig.modelClassName).start()

    val (tradeGate, marketDataProvider) = providersFactory.create(modelRuntimeConfig, executor)

    val marketStubFactory = (cfg: TickerConfig) => new MarketStubSwitcher(new MarketStub(cfg.ticker), new ExecutionMarketStub(tradeGate, cfg.ticker))

    private var model : IModel =_

    private var environment : BacktestEnvironment =_


    private val readerFactory: DefaultReaderFactory = new DefaultReaderFactory(modelConfig.dataServerRoot)
    private var backtestEnvFactory: DefaultBacktestEnvFactory =_

    if(modelRuntimeConfig.runBacktest)
        backtestEnvFactory = new DefaultBacktestEnvFactory(readerFactory, defaultTimeBoundsCalculator)
    else
        backtestEnvFactory = new DefaultBacktestEnvFactory(dummyReaderFactory, dummyTimeBoundsCalculator)

    val starter = new BacktesterSimple(backtestEnvFactory,marketStubFactory)
    environment = starter.run(modelConfig)
    model = environment.models(0)
    environment.player.close()



    private val frequencer = new Frequencer(modelConfig.backtestStepInterval, environment.player.getStepListeners(), executor)

    for (switcher <- model.stubs.map(ms => ms.asInstanceOf[MarketStubSwitcher])) {
        switcher.switchStubs()
        //to write trades to csv file
        switcher.addCallback(new TradeGateCallbackAdapter((t) => RuntimeTradeWriter.write(tradesPath, modelConfig.modelClassName, t)))
    }

    log.info("Started ")


    def start() = {
        for (k <- 0 until modelConfig.tickerConfigs.length) {
            val k1 = k
            //!!!! assumed that market data provider works in the same thread
            marketDataProvider.subscribeForTick(modelConfig.tickerConfigs(k).ticker, (q: Tick) => environment.mdDistributor.onTick(k1, q, null))
        }
        frequencer.start()
    }
}
