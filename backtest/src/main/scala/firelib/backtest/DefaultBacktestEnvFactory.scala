package firelib.backtest

import firelib.common._

class DefaultBacktestEnvFactory(readerFactory: ReadersFactory, timeBoundsCalc: TimeBoundsCalculator) extends BacktestEnvironmentFactory {
    override def apply(cfg: ModelConfig): BacktestEnvironment = {
        val intervalService = new IntervalService()

        val bound = timeBoundsCalc(cfg)

        val readers: Seq[ISimpleReader[Timed]] = readerFactory(cfg.tickerIds, bound._1)

        val mdPlayer = new MarketDataPlayer(wrapReadersWithAdapters(readers, cfg.tickerIds),bound,cfg.interval.durationMs)
        val distributor = new MarketDataDistributor(cfg.tickerIds.length, intervalService)
        mdPlayer.addStepListener(intervalService)
        mdPlayer.addListener(distributor)
        return new BacktestEnvironment(mdPlayer, distributor, bound)

    }

    def appFuncOhlc(lsn: IMarketDataListener, idx: Int, curr: Ohlc, next: Ohlc): Unit = lsn.onOhlc(idx, curr, next)

    def appFuncTick(lsn: IMarketDataListener, idx: Int, curr: Tick, next: Tick): Unit = lsn.onTick(idx, curr, next)


    def wrapReadersWithAdapters(readers: Seq[ISimpleReader[Timed]], tickerCfgs: Seq[TickerConfig]): Seq[ReaderToListenerAdapter] = {
        return readers.zipWithIndex.map(t => {
            val cfg: TickerConfig = tickerCfgs(t._2)
            if (cfg.mdType == MarketDataType.Ohlc)
                new ReaderToListenerAdapterImpl[Ohlc](t._1.asInstanceOf[ISimpleReader[Ohlc]], t._2, appFuncOhlc)
            else
                new ReaderToListenerAdapterImpl[Tick](t._1.asInstanceOf[ISimpleReader[Tick]], t._2, appFuncTick)
        })
    }


}
