package firelib.backtest

import java.time.Instant

import firelib.common._

class BacktesterSimple(marketStubFactory: String => IMarketStub = null) extends BacktesterBase(marketStubFactory) {

    override def Run(cfg: ModelConfig) {
        RunSimple(cfg);
    }

    def RunSimple(cfg: ModelConfig, runBacktest: Boolean = true): (IModel, MarketDataPlayer, MarketDataDistributor) = {

        val (startDtGmt,endDtGmt) = if(runBacktest) CalcTimeBounds(cfg) else (Instant.MAX,Instant.MAX)

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
