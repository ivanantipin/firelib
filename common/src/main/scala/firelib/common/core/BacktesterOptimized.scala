package firelib.common.core

import java.time.Instant
import java.time.temporal.ChronoUnit

import firelib.common.config.ModelConfig
import firelib.common.opt.ParamsVariator
import firelib.common.report.{ReportProcessor, backtestStatisticsCalculator, optParamsWriter, reportWriter}
import firelib.common.threading.ThreadExecutorImpl

/**
 *
 */


class BacktesterOptimized {


    def run(cfg: ModelConfig): Unit = {
        System.out.println("Starting")

        val startTime = System.currentTimeMillis()

        val reportProcessor = new ReportProcessor(backtestStatisticsCalculator,
            cfg.optConfig.optimizedMetric,
            cfg.optConfig.params.map(op => op.name),
            minNumberOfTrades = cfg.optConfig.minNumberOfTrades)

        val executor = new ThreadExecutorImpl(cfg.optConfig.threadsNumber, maxLengthOfQueue = 1).start()
        val reportExecutor = new ThreadExecutorImpl(1).start()
        val variator = new ParamsVariator(cfg.optConfig.params)



        val ctx: SimpleRunCtx = new SimpleRunCtx(cfg)
        ctx.init()
        val (startDtGmt, endDtGmt) = ctx.timeBoundsCalculator.apply(cfg)

        assert(cfg.optConfig.optimizedPeriodDays > 0, "optimized days count not set!!")

        val endOfOptimize = startDtGmt.plus(cfg.optConfig.optimizedPeriodDays, ChronoUnit.DAYS)

        System.out.println("number of models " + variator.combinations)

        while (variator.hasNext()) {
            val env = nextModelVariationsChunk(cfg, variator)
            executor.execute(() => {
                env.backtest.backtest()
                reportExecutor.execute(() => reportProcessor.process(env.models))
            })
            System.out.println(s"models scheduled for optimization ${env.models.length}")

        }

        executor.shutdown()
        reportExecutor.shutdown()

        System.out.println(s"Model optimized in ${(System.currentTimeMillis() - startTime) / 1000} sec")


        assert(reportProcessor.bestModels.length > 0, "no models get produced!!")

        val bm = reportProcessor.bestModels.last

        val env = new SimpleRunCtx(cfg)
        env.init()
        env.bindModelForParams(bm.properties)
        env.backtest.backtest()

        assert(env.models.length == 1, "no models produced")

        reportWriter.write(env.models(0), cfg, cfg.reportTargetPath)

        writeOptimizedReport(cfg, reportProcessor, endOfOptimize)

        System.out.println("Finished")
    }


    private def writeOptimizedReport(cfg: ModelConfig, reportProcessor: ReportProcessor, endOfOptimize: Instant) = {
        optParamsWriter.write(
            cfg.reportTargetPath,
            optEnd = endOfOptimize,
            estimates = reportProcessor.estimates,
            optParams = cfg.optConfig.params,
            metrics = cfg.calculatedMetrics)
    }


    private def nextModelVariationsChunk(cfg: ModelConfig, variator: ParamsVariator): SimpleRunCtx = {
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

