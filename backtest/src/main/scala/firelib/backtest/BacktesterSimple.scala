package firelib.backtest

import java.time.Instant

import firelib.common._

class BacktesterSimple(marketStubFactory: String => IMarketStub = null) extends BacktesterBase(marketStubFactory) {

    override def run(cfg: ModelConfig) {
        RunSimple(cfg)
    }

    def RunSimple(cfg: ModelConfig, backtest: Boolean = true): (IModel, MarketDataPlayer, MarketDataDistributor) = {

        val (startDtGmt,endDtGmt) = if(backtest) CalcTimeBounds(cfg) else (Instant.MAX,Instant.MAX)

        val (mdPlayer, ctx) = createModelBacktestEnv(cfg, startDtGmt, !backtest)

        val model = initModel(cfg, mdPlayer, ctx)
        if (backtest) {
            runBacktest(startDtGmt, endDtGmt, mdPlayer, cfg.interval.durationMs)
            model.onBacktestEnd
            writeModelPnlStat(cfg, model)
        }
        return (model, mdPlayer, ctx)
    }

}
