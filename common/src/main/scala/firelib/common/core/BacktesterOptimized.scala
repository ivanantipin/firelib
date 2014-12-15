package firelib.common.core

import java.time.Instant
import java.time.temporal.ChronoUnit

import firelib.common.config.ModelBacktestConfig
import firelib.common.opt.ParamsVariator
import firelib.common.report.{ReportProcessor, backtestStatisticsCalculator, optParamsWriter, reportWriter}
import firelib.common.threading.ThreadExecutorImpl

import scala.collection.mutable.ArrayBuffer


class BacktesterOptimized {


    def run(cfg: ModelBacktestConfig): Unit = {
        System.out.println("Starting")

        val startTime = System.currentTimeMillis()

        val reportProcessor = new ReportProcessor(backtestStatisticsCalculator,
            cfg.optConfig.optimizedMetric,
            cfg.optConfig.params.map(op => op.name),
            minNumberOfTrades = cfg.optConfig.minNumberOfTrades)

        //FIXME investigate rejected tasks
        val executor = new ThreadExecutorImpl(cfg.optConfig.threadsNumber).start()
        val reportExecutor = new ThreadExecutorImpl(1).start()
        val variator = new ParamsVariator(cfg.optConfig.params)



        val ctx: SimpleRunCtx = new SimpleRunCtx(cfg)
        ctx.init()
        val (startDtGmt, endDtGmt) = ctx.timeBoundsCalculator.apply(cfg)

        assert(cfg.optConfig.optimizedPeriodDays > 0, "optimized days count not set!!")

        val endOfOptimize = startDtGmt.plus(cfg.optConfig.optimizedPeriodDays, ChronoUnit.DAYS)

        println("number of models " + variator.combinations)

        while (variator.hasNext()) {
            val env = nextModelVariationsChunk(cfg, variator)
            executor.execute(() => {
                val outputs: ArrayBuffer[ModelOutput] = env.models.map(new ModelOutput(_))
                env.backtest.backtestUntil(endOfOptimize)
                reportExecutor.execute(() => reportProcessor.process(outputs))
            })
            println(s"models scheduled for optimization ${env.models.length}")

        }

        executor.shutdown()
        reportExecutor.shutdown()

        println(s"Model optimized in ${(System.currentTimeMillis() - startTime) / 1000} sec")


        assert(reportProcessor.bestModels.length > 0, "no models get produced!!")

        var output = reportProcessor.bestModels.last

        reportWriter.clearReportDir(cfg.reportTargetPath)

        if(endOfOptimize.isBefore(endDtGmt)){
            val env = new SimpleRunCtx(cfg)
            env.init()
            output = new ModelOutput(env.bindModelForParams(output.model.properties))
            env.backtest.backtest()
            assert(env.models.length == 1, "no models produced")
        }

        reportWriter.write(output, cfg, cfg.reportTargetPath)

        writeOptimizedReport(cfg, reportProcessor, endOfOptimize)

        println("Finished")
    }


    private def writeOptimizedReport(cfg: ModelBacktestConfig, reportProcessor: ReportProcessor, endOfOptimize: Instant) = {
        optParamsWriter.write(
            cfg.reportTargetPath,
            optEnd = endOfOptimize,
            estimates = reportProcessor.estimates,
            optParams = cfg.optConfig.params,
            metrics = cfg.calculatedMetrics)
    }


    private def nextModelVariationsChunk(cfg: ModelBacktestConfig, variator: ParamsVariator): SimpleRunCtx = {
        val env = new SimpleRunCtx(cfg)
        env.init()
        while (variator.hasNext()) {
            var opts = variator.next
            env.bindModelForParams(cfg.modelParams.toMap ++ opts.map(t => (t._1, t._2.toString)))
            if (env.models.length >= cfg.optConfig.batchSize) {
                return env
            }
        }
        return env
    }
}

