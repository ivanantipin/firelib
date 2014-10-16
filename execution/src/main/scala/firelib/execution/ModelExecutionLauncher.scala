package firelib.execution

import java.nio.file.Paths
import java.time.Instant

import firelib.common.config.ModelBacktestConfig
import firelib.common.core.{BindModelComponent, SimpleRunCtx, StepService}
import firelib.common.mddistributor.MarketDataDistributor
import firelib.common.misc.{jsonHelper, utils}
import firelib.common.model.Model
import firelib.common.threading.ThreadExecutorImpl
import firelib.domain.Tick
import firelib.execution.config.ModelExecutionConfig
import org.slf4j.LoggerFactory



class ModelExecutionLauncher(val execConfig: ModelExecutionConfig) {

    val log = LoggerFactory.getLogger(getClass)

    log.info("Starting config " + jsonHelper.toJsonString(execConfig))

    val tradesPath = Paths.get(execConfig.tradeLogDirectory.getOrElse("trades_live.csv")).toFile.getName

    val backtestConfig: ModelBacktestConfig = execConfig.backtestConfig

    val executor = new ThreadExecutorImpl(threadName = backtestConfig.modelClassName).start()

    val (tradeGate, marketDataProvider) = providersFactory.create(execConfig, executor)

    var backTestCtx : BindModelComponent=_

    private var model : Model =_
    private var marketDataDistributor : MarketDataDistributor =_
    private var stepService : StepService =_

    if (execConfig.runBacktestBeforeStrategyRun){
        val bctx: SimpleRunCtx = new SimpleRunCtx(backtestConfig)
        bctx.init();
        bctx.bindModelForParams(backtestConfig.modelParams.toMap)
        val endDtGmt: Instant = bctx.backtest.backtest()
        bctx.backtest.stepTill(endDtGmt, backtestConfig.stepInterval.roundTime(Instant.now()))
        model = bctx.models(0)
        marketDataDistributor = bctx.marketDataDistributor
        stepService = bctx.stepService
    }else{
        val bindCtx: BindCtx = new BindCtx(backtestConfig)
        bindCtx.init()
        bindCtx.bindModelForParams(backtestConfig.modelParams.toMap)
        model = bindCtx.models(0)
        marketDataDistributor = bindCtx.marketDataDistributor
        stepService = bindCtx.stepService

    }

    marketDataDistributor.setTickToTickFunc(utils.instanceOfClass(execConfig.tickToTickFuncClass))


    private val frequencer = new Frequencer(backtestConfig.stepInterval, (dtGmt)=>stepService.onStep(dtGmt), executor)

    model.orderManagers.foreach(om=>om.replaceTradeGate(tradeGate))


    log.info("Started ")

    def start() = {
        for (k <- 0 until backtestConfig.instruments.length) {
            val k1 = k
            //!!!! assumed that market data provider works in the same thread
            marketDataProvider.subscribeForTick(backtestConfig.instruments(k).ticker, (q: Tick) => marketDataDistributor.onTick(k1, q, null))
        }
        frequencer.start()
    }

}
