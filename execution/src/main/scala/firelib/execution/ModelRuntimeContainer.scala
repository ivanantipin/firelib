package firelib.execution

import java.nio.file.Paths
import java.time.Instant

import firelib.common._
import firelib.common.config.{InstrumentConfig, ModelConfig}
import firelib.common.core.{BindModelComponent, ModelConfigContext, OnContextInited, SimpleRunCtx, StepService, StepServiceComponent}
import firelib.common.interval.IntervalServiceComponent
import firelib.common.marketstub.{MarketStub, MarketStubFactoryComponent, MarketStubImpl}
import firelib.common.mddistributor.{MarketDataDistributor, MarketDataDistributorComponent}
import firelib.common.misc.{TickToPriceConverterComponent, jsonHelper}
import firelib.common.model.Model
import firelib.common.reader.{ReadersFactory, SimpleReader}
import firelib.common.threading.ThreadExecutorImpl
import firelib.common.timeboundscalc.TimeBoundsCalculator
import firelib.domain.{Tick, Timed}
import firelib.execution.config.ModelRuntimeConfig
import org.slf4j.LoggerFactory


class BindCtx(val modelConfig : ModelConfig) extends OnContextInited
with BindModelComponent
with ModelConfigContext
with MarketDataDistributorComponent
with MarketStubFactoryComponent
with StepServiceComponent
with IntervalServiceComponent
with TickToPriceConverterComponent{

}

class ModelRuntimeContainer(val modelRuntimeConfig: ModelRuntimeConfig) {

    val log = LoggerFactory.getLogger(getClass)

    log.info("Starting config " + jsonHelper.toJsonString(modelRuntimeConfig))

    val tradesPath = Paths.get(modelRuntimeConfig.tradeLogDirectory.getOrElse("trades_live.csv")).toFile.getName

    val modelConfig: ModelConfig = modelRuntimeConfig.modelConfig

    val executor = new ThreadExecutorImpl(threadName = modelRuntimeConfig.modelConfig.modelClassName).start()

    val (tradeGate, marketDataProvider) = providersFactory.create(modelRuntimeConfig, executor)

    val marketStubSwitcherFactory : (InstrumentConfig=>MarketStub) = (cfg: InstrumentConfig) => new MarketStubSwitcher(new MarketStubImpl(cfg.ticker), new ExecutionMarketStub(tradeGate, cfg.ticker))

    var backTestCtx : BindModelComponent=_

    private var model : Model =_
    private var marketDataDistributor : MarketDataDistributor =_
    private var stepService : StepService =_

    if (modelRuntimeConfig.runBacktest){

        val bctx: SimpleRunCtx = new SimpleRunCtx(modelConfig)
        bctx.init();
        bctx.bindModelForParams(modelConfig.modelParams.toMap)
        bctx.backtest.backtest()
        model = bctx.models(0)
        marketDataDistributor = bctx.marketDataDistributor
        stepService = bctx.stepService


    }else{
        val bindCtx: BindCtx = new BindCtx(modelConfig){
            override def tickToPriceConverterFactory = (cfg)=>(t)=>(t.getBid + t.getAsk)/2
            override val marketStubFactory : (InstrumentConfig=>MarketStub) = marketStubSwitcherFactory
        }
        bindCtx.init()
        bindCtx.bindModelForParams(modelConfig.modelParams.toMap)
        model = bindCtx.models(0)
        marketDataDistributor = bindCtx.marketDataDistributor
        stepService = bindCtx.stepService

    }



    private val frequencer = new Frequencer(modelConfig.stepInterval, (dtGmt)=>stepService.onStep(dtGmt), executor)

    for (switcher <- model.stubs.map(ms => ms.asInstanceOf[MarketStubSwitcher])) {
        switcher.switchStubs()
        //to write trades to csv file
        switcher.addCallback(new TradeGateCallbackAdapter((t) => runtimeTradeWriter.write(tradesPath, modelConfig.modelClassName, t)))
    }

    log.info("Started ")

    def start() = {
        for (k <- 0 until modelConfig.instruments.length) {
            val k1 = k
            //!!!! assumed that market data provider works in the same thread
            marketDataProvider.subscribeForTick(modelConfig.instruments(k).ticker, (q: Tick) => marketDataDistributor.onTick(k1, q, null))
        }
        frequencer.start()
    }

    object dummyReaderFactory extends ReadersFactory{
        override def apply(cfgs: Seq[InstrumentConfig], startTime: Instant): Seq[SimpleReader[Timed]] =  cfgs.map(_=>dummyReader)
    }

    object dummyTimeBoundsCalculator extends TimeBoundsCalculator{
        override def apply(cfg: ModelConfig): (Instant, Instant) = (Instant.MAX,Instant.MIN)
    }

    /**
     * dummy reader used when no backtest required just model initialization
     */
    object dummyReader extends SimpleReader[Timed] {

        override def seek(time: Instant): Boolean = true

        override def endTime(): Instant = Instant.MAX

        override def read(): Boolean = false

        override def startTime(): Instant = Instant.MAX

        override def current: Timed = null

        override def close(): Unit = {}
    }

}
