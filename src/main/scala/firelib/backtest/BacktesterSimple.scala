package firelib.backtest

import org.joda.time.DateTime

class BacktesterSimple(marketStubFactory: String => IMarketStub = null) extends BacktesterBase(marketStubFactory) {

    override def Run(cfg: ModelConfig) {
        RunSimple(cfg);
    }

    def RunSimple(cfg: ModelConfig, runBacktest: Boolean = true): (IModel, MarketDataPlayer, MarketDataDistributor) = {
        var endDtGmt = new DateTime(Int.MaxValue);
        var startDtGmt = new DateTime(Int.MaxValue)
        if (runBacktest) {
            val startDtGmt, endDtGmt = CalcTimeBounds(cfg);
        }

        val (mdPlayer, ctx) = CreateModelBacktestEnvironment(cfg, startDtGmt, !runBacktest);

        val model = InitModel(cfg, mdPlayer, ctx);
        if (runBacktest) {
            RunBacktest(startDtGmt, endDtGmt, mdPlayer, cfg.interval.durationMs);
            model.onBacktestEnd
            WriteModelPnlStat(cfg, model);
        }
        return (model, mdPlayer, ctx)
    }

}
