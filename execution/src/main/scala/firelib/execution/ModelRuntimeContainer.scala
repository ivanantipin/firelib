package firelib.execution

import java.nio.file.Paths
import java.time.Instant

import firelib.common._
import firelib.common.config.{ModelConfig, TickerConfig}
import firelib.common.core.SimpleRunCtx
import firelib.common.marketstub.{MarketStub, MarketStubImpl}
import firelib.common.misc.jsonHelper
import firelib.common.model.Model
import firelib.common.reader.{ReadersFactory, SimpleReader}
import firelib.common.threading.ThreadExecutorImpl
import firelib.common.timeboundscalc.TimeBoundsCalculator
import firelib.domain.{Tick, Timed}
import firelib.execution.config.ModelRuntimeConfig
import org.slf4j.LoggerFactory


class ModelRuntimeContainer(val modelRuntimeConfig: ModelRuntimeConfig) {

    val log = LoggerFactory.getLogger(getClass)

    log.info("Starting config " + jsonHelper.toJsonString(modelRuntimeConfig))

    val tradesPath = Paths.get(modelRuntimeConfig.tradeLogDirectory.getOrElse("trades_live.csv")).toFile.getName

    val modelConfig: ModelConfig = modelRuntimeConfig.modelConfig

    val executor = new ThreadExecutorImpl(threadName = modelRuntimeConfig.modelConfig.modelClassName).start()

    val (tradeGate, marketDataProvider) = providersFactory.create(modelRuntimeConfig, executor)

    val marketStubSwitcherFactory : (TickerConfig=>MarketStub) = (cfg: TickerConfig) => new MarketStubSwitcher(new MarketStubImpl(cfg.ticker), new ExecutionMarketStub(tradeGate, cfg.ticker))

    var backTestCtx : SimpleRunCtx=_

    if (modelRuntimeConfig.runBacktest)
        backTestCtx = new SimpleRunCtx(modelConfig.dataServerRoot) {
            override val marketStubFactory = marketStubSwitcherFactory
        }
    else
        backTestCtx = new SimpleRunCtx(modelConfig.dataServerRoot) {
            override val timeBoundsCalculator = dummyTimeBoundsCalculator
            override val readersFactory = dummyReaderFactory
            override val marketStubFactory = marketStubSwitcherFactory
        }

    val env = backTestCtx.backtesterSimple.run(modelConfig)

    private val model = env.models(0)

    private val frequencer = new Frequencer(modelConfig.backtestStepInterval, env.stepListeners, executor)

    for (switcher <- model.stubs.map(ms => ms.asInstanceOf[MarketStubSwitcher])) {
        switcher.switchStubs()
        //to write trades to csv file
        switcher.addCallback(new TradeGateCallbackAdapter((t) => runtimeTradeWriter.write(tradesPath, modelConfig.modelClassName, t)))
    }

    log.info("Started ")

    def start() = {
        for (k <- 0 until modelConfig.tickerConfigs.length) {
            val k1 = k
            //!!!! assumed that market data provider works in the same thread
            marketDataProvider.subscribeForTick(modelConfig.tickerConfigs(k).ticker, (q: Tick) => env.mdDistr.onTick(k1, q, null))
        }
        frequencer.start()
    }

    object dummyReaderFactory extends ReadersFactory{
        override def apply(cfgs: Seq[TickerConfig], startTime: Instant): Seq[SimpleReader[Timed]] =  cfgs.map(_=>dummyReader)
    }

    object dummyTimeBoundsCalculator extends TimeBoundsCalculator{
        override def apply(cfg: ModelConfig): (Instant, Instant) = (Instant.MAX,Instant.MIN)
    }

    /**
     * dummy reader used when no backtest required just model initialization
     */
    val dummyReader: SimpleReader[Timed] = new SimpleReader[Timed] {

        override def seek(time: Instant): Boolean = true

        override def endTime(): Instant = Instant.MAX

        override def read(): Boolean = false

        override def startTime(): Instant = Instant.MAX

        override def current: Timed = null

        override def close(): Unit = {}
    }

}
