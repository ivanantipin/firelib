package firelib.common.core

import java.time.Instant

import firelib.common.config.InstrumentConfig
import firelib.common.mddistributor.MarketDataDistributorComponent
import firelib.common.reader.{ReaderToListenerAdapter, ReaderToListenerAdapterImpl, ReadersFactoryComponent, SimpleReader}
import firelib.common.timeboundscalc.TimeBoundsCalculatorComponent
import firelib.common.{MarketDataListener, MarketDataType}
import firelib.domain.{Ohlc, Tick, Timed}

trait BacktestComponent{

    this : TimeBoundsCalculatorComponent
    with ModelConfigContext
    with ReadersFactoryComponent
    with StepServiceComponent
    with MarketDataDistributorComponent
    with BindModelComponent=>

    val backtest = new Backtest

    class Backtest{
        def backtest() {
            val bounds = timeBoundsCalculator.apply(modelConfig)
            val readers: Seq[SimpleReader[Timed]] = readersFactory.apply(modelConfig.instruments, bounds._1)
            var tickerPlayers: Seq[ReaderToListenerAdapter]= wrapReadersWithAdapters(readers, modelConfig.instruments)
            tickerPlayers.foreach(_.addListener(marketDataDistributor))
            var dtGmtcur = bounds._1
            while (dtGmtcur.isBefore(bounds._2) && step(tickerPlayers,dtGmtcur)) {
                dtGmtcur = dtGmtcur.plusMillis(modelConfig.stepInterval.durationMs)
            }
            models.foreach(_.onBacktestEnd())
            tickerPlayers.foreach(_.close())
        }

        def step(tickerPlayers: Seq[ReaderToListenerAdapter], chunkEndGmt: Instant): Boolean = {
            for (i <- 0 until tickerPlayers.length) {
                if (!tickerPlayers(i).readUntil(chunkEndGmt)) {
                    return false
                }
            }
            stepService.onStep(chunkEndGmt)
            return true
        }

        def appFuncOhlc(lsn: MarketDataListener, idx: Int, curr: Ohlc, next: Ohlc): Unit = lsn.onOhlc(idx, curr, next)

        def appFuncTick(lsn: MarketDataListener, idx: Int, curr: Tick, next: Tick): Unit = lsn.onTick(idx, curr, next)



        def wrapReadersWithAdapters(readers: Seq[SimpleReader[Timed]], tickerCfgs: Seq[InstrumentConfig]): Seq[ReaderToListenerAdapter] = {
            return readers.zipWithIndex.map(t => {
                val cfg: InstrumentConfig = tickerCfgs(t._2)
                if (cfg.mdType == MarketDataType.Ohlc)
                    new ReaderToListenerAdapterImpl[Ohlc](t._1.asInstanceOf[SimpleReader[Ohlc]], t._2, appFuncOhlc)
                else
                    new ReaderToListenerAdapterImpl[Tick](t._1.asInstanceOf[SimpleReader[Tick]], t._2, appFuncTick)
            })
        }

    }


}
