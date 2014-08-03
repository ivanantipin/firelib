package firelib.backtest

import java.time.Instant
import java.time.temporal.ChronoUnit

import firelib.common._
import firelib.utils.OptParamsWriter

import scala.collection.mutable.ArrayBuffer
import scala.util.control._

class BacktesterOptimized extends BacktesterBase {


    override def Run(cfg: ModelConfig) = {
        System.out.println("Starting")

        var startTime = System.currentTimeMillis();

        val reportProcessor = new ReportProcessor(BacktestStatisticsCalculator.CalculateStatisticsForCases,
            cfg.optimizedMetric,
            cfg.optParams.map(op => op.Name),
            minNumberOfTrades = cfg.optMinNumberOfTrades);

        val executor = new ThreadExecutor(cfg.optThreadNumber, maxLengthOfQueue = 1).Start()
        val reportExecutor = new ThreadExecutor(1).Start();
        val variator = new ParamsVariator(cfg.optParams);


        val (startDtGmt, endDtGmt) = CalcTimeBounds(cfg);

        assert(cfg.optimizedPeriodDays > 0 , "optimized days count not set!!")

        val endOfOptimize = startDtGmt.plus(cfg.optimizedPeriodDays, ChronoUnit.DAYS);

        System.out.println("number of models " + variator.Combinations)

        while (true) {
            val (mdPlayer, distributor) = CreateModelBacktestEnvironment(cfg, startDtGmt)
            val models = NextModelVariationsChunk(cfg, variator, mdPlayer, distributor)
            if (models.length == 0) {
                Breaks.break
            }
            val task = new BacktestTask(startDtGmt, endOfOptimize, mdPlayer, cfg.interval.durationMs, models, (mod) => reportExecutor.Execute(reportProcessor.Process(mod)));

            executor.Execute(task.run)

            System.out.println("models scheduled " + models.length)

        }

        executor.Stop();
        reportExecutor.Stop();

        System.out.println("Model optimized in " + (System.currentTimeMillis() - startTime) / 1000 + " sec. ")


        assert(reportProcessor.BestModels.length > 0, "no models get produced!!")

        var bm = reportProcessor.BestModels.last
        val (mdPlayer, distributor) = CreateModelBacktestEnvironment(cfg, startDtGmt);
        var model = initModelWithCustomProps(cfg, mdPlayer, distributor, bm.properties);
        RunBacktest(startDtGmt, endDtGmt, mdPlayer, cfg.interval.durationMs);


        writeModelPnlStat(cfg, model);
        WriteOptimizedReport(cfg, reportProcessor, endOfOptimize);

        System.out.println("Finished")
    }

    private class BacktestTask(val StartDtGmt:Instant, val EndDtGmt:Instant, val MdPlayer: MarketDataPlayer, stepMs: Int, Models: Seq[IModel], callback: Seq[IModel] => Unit) {

        def run() = {
            RunBacktest(StartDtGmt, EndDtGmt, MdPlayer, stepMs);
            callback(Models);
        }
    }

    private def WriteOptimizedReport(cfg: ModelConfig, reportProcessor: ReportProcessor, endOfOptimize:Instant) = {
        OptParamsWriter.write(
            cfg.reportRoot,
            OptEnd = endOfOptimize,
            Estimates = reportProcessor.Estimates,
            optParams = cfg.optParams,
            metrics = cfg.calculatedMetrics);
    }


    private def NextModelVariationsChunk(cfg: ModelConfig, variator: ParamsVariator, mdPlayer: MarketDataPlayer, ctx: MarketDataDistributor): Seq[IModel] = {

        var models = new ArrayBuffer[IModel]();

        var varr = variator.Next

        while (varr != null) {
            var model = initModel(cfg, mdPlayer, ctx, varr);

            if (model.hasValidProps) {
                models += model
                if (models.length >= cfg.optBatchSize) {
                    return models;
                }
            }
            varr = variator.Next
        }
        return models;

    }
}
