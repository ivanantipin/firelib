package firelib.execution

import java.nio.file.Paths
import java.time.Instant

import firelib.common.config.ModelBacktestConfig
import firelib.common.core.SimpleRunCtx
import firelib.common.misc.{jsonHelper, utils}
import firelib.common.model.Model
import firelib.common.report.{StreamOrderWriter, StreamTradeCaseWriter}
import firelib.common.threading.ThreadExecutorImpl
import firelib.common.timeservice.TimeServiceReal
import firelib.domain.Tick
import firelib.execution.config.ModelExecutionConfig
import org.slf4j.LoggerFactory


class ModelExecutionLauncher(val execConfig: ModelExecutionConfig) {

    val log = LoggerFactory.getLogger(getClass)

    log.info("Starting config " + jsonHelper.toJsonString(execConfig))

    val tradesPath = Paths.get(execConfig.tradeLogDirectory.getOrElse("trades_live.csv"))

    val ordersPath = Paths.get(execConfig.tradeLogDirectory.getOrElse("orders.csv"))

    val backtestConfig: ModelBacktestConfig = execConfig.backtestConfig

    val executor = new ThreadExecutorImpl(threadName = backtestConfig.modelClassName).start()

    val (tradeGate, marketDataProvider) = providersFactory.create(execConfig, executor)

    val bctx: SimpleRunCtx = new SimpleRunCtx(backtestConfig)
    bctx.init();
    private val model : Model = bctx.bindModelForParams(backtestConfig.modelParams.toMap)


    if (execConfig.runBacktestBeforeStrategyRun){
        bctx.backtest.backtest()
        bctx.backtest.stepUntil(backtestConfig.stepInterval.roundTime(Instant.now()))
    }

    bctx.marketDataDistributor.setTickTransformFunction(utils.instanceOfClass(execConfig.tickToTickFuncClass))

    private val frequencer = new Frequencer(backtestConfig.stepInterval, (dtGmt)=>bctx.intervalService.onStep(dtGmt), executor)

    bctx.tradeGate = tradeGate

    bctx.timeService = new TimeServiceReal

    private val tradeWriter: StreamTradeCaseWriter = new StreamTradeCaseWriter(tradesPath, List[String]())

    model.orderManagers.foreach(om=>om.listenTrades(tradeWriter))

    private val orderWriter: StreamOrderWriter = new StreamOrderWriter(ordersPath)

    model.orderManagers.foreach(om=>om.listenOrders(s=>orderWriter.apply(s.order)))

    log.info("Started ")

    def start() = {
        for (k <- 0 until backtestConfig.instruments.length) {
            val k1 = k
            //!!!! assumed that market data provider works in the same thread
            marketDataProvider.subscribeForTick(backtestConfig.instruments(k).ticker, (q: Tick) => bctx.marketDataDistributor.onTick(k1, q))
        }
        frequencer.start()
    }

}
