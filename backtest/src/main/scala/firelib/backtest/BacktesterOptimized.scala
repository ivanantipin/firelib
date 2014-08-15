package firelib.backtest

import java.time.Instant
import java.time.temporal.ChronoUnit

import firelib.common._
import firelib.utils.{OptParamsWriter, ReportWriter}

import scala.collection.mutable.ArrayBuffer

class BacktesterOptimized (backtestEnvFactory : BacktestEnvironmentFactory, marketStubFactory : MarketStubFactory ) {


     def run(cfg: ModelConfig) : Unit = {
        System.out.println("Starting")

        val startTime = System.currentTimeMillis()

        val reportProcessor = new ReportProcessorImpl(backtestStatisticsCalculator,
            cfg.optimizedMetric,
            cfg.optParams.map(op => op.Name),
            minNumberOfTrades = cfg.optMinNumberOfTrades)

        val executor = new ThreadExecutor(cfg.optThreadNumber, maxLengthOfQueue = 1).start()
        val reportExecutor = new ThreadExecutor(1).start()
        val variator = new ParamsVariator(cfg.optParams)
        

        val (startDtGmt, endDtGmt) = defaultTimeBoundsCalculator(cfg)

        assert(cfg.optimizedPeriodDays > 0 , "optimized days count not set!!")

        val endOfOptimize = startDtGmt.plus(cfg.optimizedPeriodDays, ChronoUnit.DAYS)

        System.out.println("number of models " + variator.combinations)

        while (variator.hasNext()) {
            val env = nextModelVariationsChunk(cfg, variator)
            executor.execute(()=>{
                env.backtest()
                reportExecutor.execute(()=>reportProcessor.process(env.models))
            })
            System.out.println("models scheduled " + env.models.length)

        }

        executor.shutdown()
        reportExecutor.shutdown()

        System.out.println("Model optimized in " + (System.currentTimeMillis() - startTime) / 1000 + " sec. ")


        assert(reportProcessor.bestModels.length > 0, "no models get produced!!")

        val bm = reportProcessor.bestModels.last
        val env = backtestEnvFactory(cfg)
        val model = cfg.newInstance()
        val stubs: ArrayBuffer[IMarketStub] = cfg.tickerIds.map(marketStubFactory)
        env.bindModelIntoEnv(model,stubs,bm.properties)
        env.backtest()

        ReportWriter.write(model, cfg, cfg.reportRoot)

        writeOptimizedReport(cfg, reportProcessor, endOfOptimize)

        System.out.println("Finished")
    }


    private def writeOptimizedReport(cfg: ModelConfig, reportProcessor: ReportProcessorImpl, endOfOptimize:Instant) = {
        OptParamsWriter.write(
            cfg.reportRoot,
            optEnd = endOfOptimize,
            estimates = reportProcessor.estimates,
            optParams = cfg.optParams,
            metrics = cfg.calculatedMetrics)
    }


    private def nextModelVariationsChunk(cfg: ModelConfig, variator: ParamsVariator): BacktestEnvironment = {
        val env: BacktestEnvironment = backtestEnvFactory(cfg)
        while (variator.hasNext()) {
            var opts = variator.next
            val model: IModel = cfg.newInstance()
            val stubs: Seq[IMarketStub] = cfg.tickerIds.map(marketStubFactory)
            env.bindModelIntoEnv(model,stubs,cfg.customParams.toMap ++ opts.map(t => (t._1, t._2.toString)))

            if (env.models.length >= cfg.optBatchSize) {
                return env
            }
        }
        return env
    }
}
