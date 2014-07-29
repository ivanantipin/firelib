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
            cfg.OptimizedMetric,
            cfg.OptParams.map(op => op.Name),
            minNumberOfTrades = cfg.OptMinNumberOfTrades);

        val executor = new ThreadExecutor(cfg.OptThreadNumber, maxLengthOfQueue = 1).Start()
        val reportExecutor = new ThreadExecutor(1).Start();
        val variator = new ParamsVariator(cfg.OptParams);


        val (startDtGmt, endDtGmt) = CalcTimeBounds(cfg);

        assert(cfg.OptimizedPeriodDays > 0 , "optimized days count not set!!")

        val endOfOptimize = startDtGmt.plus(cfg.OptimizedPeriodDays, ChronoUnit.DAYS);

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
        var model = InitModelWithCustomProps(cfg, mdPlayer, distributor, bm.properties);
        RunBacktest(startDtGmt, endDtGmt, mdPlayer, cfg.interval.durationMs);


        WriteModelPnlStat(cfg, model);
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
            cfg.ReportRoot,
            OptEnd = endOfOptimize,
            Estimates = reportProcessor.Estimates,
            optParams = cfg.OptParams,
            metrics = cfg.CalculatedMetrics);
    }


    private def NextModelVariationsChunk(cfg: ModelConfig, variator: ParamsVariator, mdPlayer: MarketDataPlayer, ctx: MarketDataDistributor): Seq[IModel] = {

        var models = new ArrayBuffer[IModel]();

        var varr = variator.Next

        while (varr != null) {
            var model = InitModel(cfg, mdPlayer, ctx, varr);

            if (model.hasValidProps) {
                models += model
                if (models.length >= cfg.OptBatchSize) {
                    return models;
                }
            }
            varr = variator.Next
        }
        return models;

    }
}
