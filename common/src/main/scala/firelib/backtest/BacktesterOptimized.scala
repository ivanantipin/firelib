package firelib.backtest

import java.time.Instant
import java.time.temporal.ChronoUnit

import firelib.common._
import firelib.report.{OptParamsWriter, reportWriter}

import scala.collection.mutable.ArrayBuffer

/**
 *
 * @param backtestEnvFactory
 * @param marketStubFactory
 */
class BacktesterOptimized (backtestEnvFactory : BacktestEnvironmentFactory, marketStubFactory : MarketStubFactory ) {


     def run(cfg: ModelConfig) : Unit = {
        System.out.println("Starting")

        val startTime = System.currentTimeMillis()

        val reportProcessor = new ReportProcessor(backtestStatisticsCalculator,
            cfg.optimizedMetric,
            cfg.optParams.map(op => op.name),
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
            System.out.println(s"models scheduled for optimization ${env.models.length}")

        }

        executor.shutdown()
        reportExecutor.shutdown()

        System.out.println(s"Model optimized in ${(System.currentTimeMillis() - startTime) / 1000} sec")


        assert(reportProcessor.bestModels.length > 0, "no models get produced!!")

        val bm = reportProcessor.bestModels.last
        val env = backtestEnvFactory(cfg)
        val model = cfg.newModelInstance()
        val stubs: ArrayBuffer[IMarketStub] = cfg.tickerConfigs.map(marketStubFactory)
        env.bindModelIntoEnv(model,stubs,bm.properties)
        env.backtest()

        reportWriter.write(model, cfg, cfg.reportTargetPath)

        writeOptimizedReport(cfg, reportProcessor, endOfOptimize)

        System.out.println("Finished")
    }


    private def writeOptimizedReport(cfg: ModelConfig, reportProcessor: ReportProcessor, endOfOptimize:Instant) = {
        OptParamsWriter.write(
            cfg.reportTargetPath,
            optEnd = endOfOptimize,
            estimates = reportProcessor.estimates,
            optParams = cfg.optParams,
            metrics = cfg.calculatedMetrics)
    }


    private def nextModelVariationsChunk(cfg: ModelConfig, variator: ParamsVariator): BacktestEnvironment = {
        val env: BacktestEnvironment = backtestEnvFactory(cfg)
        while (variator.hasNext()) {
            var opts = variator.next
            val model: IModel = cfg.newModelInstance()
            val stubs: Seq[IMarketStub] = cfg.tickerConfigs.map(marketStubFactory)
            env.bindModelIntoEnv(model,stubs,cfg.customParams.toMap ++ opts.map(t => (t._1, t._2.toString)))

            if (env.models.length >= cfg.optBatchSize) {
                return env
            }
        }
        return env
    }
}
