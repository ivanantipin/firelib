package firelib.backtest

import org.joda.time.DateTime

import scala.collection.mutable.ArrayBuffer
import scala.util.control._

class BacktesterOptimized extends BacktesterBase {

    var reportProcessor: ReportProcessor

    override def Run(cfg: ModelConfig) = {
        //Console.WriteLine("Starting");
        var startTime = DateTime.now();

        reportProcessor = new ReportProcessor(BacktestStatisticsCalculator.CalculateStatisticsForCases,
            cfg.OptimizedMetric,
            cfg.OptParams.map(op => op.Name),
            minNumberOfTrades = cfg.OptMinNumberOfTrades);


        val executor = new ThreadExecutor(cfg.OptThreadNumber, maxLengthOfQueue = 1)
        executor.Start();

        // ReSharper disable RedundantArgumentDefaultValue
        var reportExecutor = new ThreadExecutor(1);
        // ReSharper restore RedundantArgumentDefaultValue

        reportExecutor.Start();

        var variator = new ParamsVariator(cfg.OptParams);

        var ctx: MarketDataDistributor = null

        var mdPlayer: MarketDataPlayer = null

        var interval = cfg.interval
        val (startDtGmt, endDtGmt) = CalcTimeBounds(cfg);


        if (cfg.OptimizedPeriodDays == -1) {
            throw new Exception("optimized days count not set!!");
        }

        val endOfOptimize = startDtGmt.plusDays(cfg.OptimizedPeriodDays);

        System.out.println("number of models " + variator.Combinations)

        while (true) {
            (mdPlayer, ctx) = CreateModelBacktestEnvironment(cfg, startDtGmt);
            var models = NextModelVariationsChunk(cfg, variator, mdPlayer, ctx);
            if (models.length == 0) {
                Breaks.break
            }
            val bt = new BacktestTask(startDtGmt, endOfOptimize, mdPlayer, interval.durationMs, models, (mod) => reportExecutor.Execute(Unit => reportProcessor.Process(mod));

            executor.Execute(Unit => bt.run)

            System.out.println("models scheduled " + models.length)

        }

        executor.Stop();
        reportExecutor.Stop();

        System.out.println("Model optimized in " + (DateTime.now().getMillis - startTime.getMillis) / 1000 + " sec. ")


        if (reportProcessor.BestModels.length == 0) {
            throw new Exception("no best model produced!!");
        }

        var bm = reportProcessor.BestModels.last
        (mdPlayer, ctx) = CreateModelBacktestEnvironment(cfg, startDtGmt);
        var model = InitModelWithCustomProps(cfg, mdPlayer, ctx, bm.properties);
        RunBacktest(startDtGmt, endDtGmt, mdPlayer, interval.durationMs);


        WriteModelPnlStat(cfg, model);
        WriteOptimizedReport(cfg, reportProcessor, endOfOptimize);

        System.out.println("Finished")
    }

    private class BacktestTask(val StartDtGmt: DateTime, val EndDtGmt: DateTime, val MdPlayer: MarketDataPlayer, stepMs: Int, Models: Seq[IModel], callback: Seq[IModel] => Unit) {

        def run() = {
            RunBacktest(StartDtGmt, EndDtGmt, MdPlayer, stepMs);
            if (callback != null) {
                callback(Models);
            }
        }
    }

    private def WriteOptimizedReport(cfg: ModelConfig, reportProcessor: ReportProcessor, endOfOptimize: DateTime) = {
        var optParamsWriter = new OptParamsWriter {
            Estimates = reportProcessor.Estimates,
            Merics = cfg.CalculatedMetrics,
            Opts = cfg.OptParams,
            OptEnd = endOfOptimize
        };
        optParamsWriter.Write(cfg.ReportRoot);
    }


    private def NextModelVariationsChunk(cfg: ModelConfig, variator: ParamsVariator, mdPlayer: MarketDataPlayer, ctx: MarketDataDistributor): Seq[IModel] = {
        var varr: Map[String, Int] = null
        var models = new ArrayBuffer[IModel]();

        while ((varr = variator.Next) != null) {
            var model = InitModel(cfg, mdPlayer, ctx, varr);

            if (model.hasValidProps) {
                models += model
                if (models.length >= cfg.OptBatchSize) {
                    return models;
                }
            }
        }
        return models;

    }
}
