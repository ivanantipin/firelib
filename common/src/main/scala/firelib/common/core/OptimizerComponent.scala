package firelib.common.core

import java.time.Instant
import java.time.temporal.ChronoUnit

import firelib.common.config.ModelConfig
import firelib.common.marketstub.{MarketStub, MarketStubFactoryComponent}
import firelib.common.model.Model
import firelib.common.opt.ParamsVariator
import firelib.common.report.{OptParamsWriter, ReportProcessor, backtestStatisticsCalculator, reportWriter}
import firelib.common.threading.ThreadExecutorImpl
import firelib.common.timeboundscalc.TimeBoundsCalculatorComponent

import scala.collection.mutable.ArrayBuffer

/**
 *
 */

trait OptimizerComponent{

    this : EnvFactoryComponent with MarketStubFactoryComponent with TimeBoundsCalculatorComponent =>

    val optimizer = new Optimizer()

    class Optimizer {


        def run(cfg: ModelConfig) : Unit = {
            System.out.println("Starting")

            val startTime = System.currentTimeMillis()

            val reportProcessor = new ReportProcessor(backtestStatisticsCalculator,
                cfg.optimizedMetric,
                cfg.optParams.map(op => op.name),
                minNumberOfTrades = cfg.optMinNumberOfTrades)

            val executor = new ThreadExecutorImpl(cfg.optThreadNumber, maxLengthOfQueue = 1).start()
            val reportExecutor = new ThreadExecutorImpl(1).start()
            val variator = new ParamsVariator(cfg.optParams)


            val (startDtGmt, endDtGmt) = timeBoundsCalculator.apply(cfg)

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
            val env = envFactory.apply(cfg)
            val model = cfg.newModelInstance()
            val stubs: ArrayBuffer[MarketStub] = cfg.tickerConfigs.map(marketStubFactory)
            env.bindModel(model,stubs,bm.properties)
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
            val env: BacktestEnvironment = envFactory.apply(cfg)
            while (variator.hasNext()) {
                var opts = variator.next
                val model: Model = cfg.newModelInstance()
                val stubs: Seq[MarketStub] = cfg.tickerConfigs.map(marketStubFactory)
                env.bindModel(model,stubs,cfg.customParams.toMap ++ opts.map(t => (t._1, t._2.toString)))

                if (env.models.length >= cfg.optBatchSize) {
                    return env
                }
            }
            return env
        }
    }

}
