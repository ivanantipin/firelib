package firelib.backtest

import java.time.Instant
import java.time.temporal.ChronoUnit

import firelib.common._
import firelib.utils.OptParamsWriter

import scala.collection.mutable.ArrayBuffer
import scala.util.control._

class BacktesterOptimized extends BacktesterBase {


    override def run(cfg: ModelConfig) = {
        System.out.println("Starting")

        val startTime = System.currentTimeMillis()

        val reportProcessor = new ReportProcessor(BacktestStatisticsCalculator.CalculateStatisticsForCases,
            cfg.optimizedMetric,
            cfg.optParams.map(op => op.Name),
            minNumberOfTrades = cfg.optMinNumberOfTrades)

        val executor = new ThreadExecutor(cfg.optThreadNumber, maxLengthOfQueue = 1).start()
        val reportExecutor = new ThreadExecutor(1).start()
        val variator = new ParamsVariator(cfg.optParams)


        val (startDtGmt, endDtGmt) = CalcTimeBounds(cfg)

        assert(cfg.optimizedPeriodDays > 0 , "optimized days count not set!!")

        val endOfOptimize = startDtGmt.plus(cfg.optimizedPeriodDays, ChronoUnit.DAYS)

        System.out.println("number of models " + variator.Combinations)

        while (true) {
            val (mdPlayer, distributor) = createModelBacktestEnv(cfg, startDtGmt)
            val models = nextModelVariationsChunk(cfg, variator, mdPlayer, distributor)
            if (models.length == 0) {
                Breaks.break
            }
            val task = new BacktestTask(startDtGmt, endOfOptimize, mdPlayer, cfg.interval.durationMs, models, (mod) => reportExecutor.execute(reportProcessor.process(mod)))

            executor.execute(task.run)

            System.out.println("models scheduled " + models.length)

        }

        executor.stop()
        reportExecutor.stop()

        System.out.println("Model optimized in " + (System.currentTimeMillis() - startTime) / 1000 + " sec. ")


        assert(reportProcessor.BestModels.length > 0, "no models get produced!!")

        val bm = reportProcessor.BestModels.last
        val (mdPlayer, distributor) = createModelBacktestEnv(cfg, startDtGmt)
        val model = initModelWithCustomProps(cfg, mdPlayer, distributor, bm.properties)
        runBacktest(startDtGmt, endDtGmt, mdPlayer, cfg.interval.durationMs)


        writeModelPnlStat(cfg, model)
        writeOptimizedReport(cfg, reportProcessor, endOfOptimize)

        System.out.println("Finished")
    }

    private class BacktestTask(val StartDtGmt:Instant, val EndDtGmt:Instant, val MdPlayer: MarketDataPlayer, stepMs: Int, Models: Seq[IModel], callback: Seq[IModel] => Unit) {

        def run() = {
            runBacktest(StartDtGmt, EndDtGmt, MdPlayer, stepMs)
            callback(Models)
        }
    }

    private def writeOptimizedReport(cfg: ModelConfig, reportProcessor: ReportProcessor, endOfOptimize:Instant) = {
        OptParamsWriter.write(
            cfg.reportRoot,
            OptEnd = endOfOptimize,
            Estimates = reportProcessor.Estimates,
            optParams = cfg.optParams,
            metrics = cfg.calculatedMetrics)
    }


    private def nextModelVariationsChunk(cfg: ModelConfig, variator: ParamsVariator, mdPlayer: MarketDataPlayer, ctx: MarketDataDistributor): Seq[IModel] = {

        var models = new ArrayBuffer[IModel]()

        var varr = variator.Next

        while (varr != null) {
            var model = initModel(cfg, mdPlayer, ctx, varr)

            if (model.hasValidProps) {
                models += model
                if (models.length >= cfg.optBatchSize) {
                    return models
                }
            }
            varr = variator.Next
        }
        return models

    }
}
