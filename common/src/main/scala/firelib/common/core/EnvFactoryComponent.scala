package firelib.common.core

import firelib.common.MarketDataListener
import firelib.common.config.{ModelConfig, TickerConfig}
import firelib.common.interval.{IntervalServiceComponent, IntervalServiceImpl}
import firelib.common.mddistributor.MarketDataDistributorComponent
import firelib.common.reader.{ReaderToListenerAdapter, ReaderToListenerAdapterImpl, ReadersFactoryComponent, SimpleReader}
import firelib.common.timeboundscalc.TimeBoundsCalculatorComponent
import firelib.common.MarketDataType
import firelib.domain.{Ohlc, Tick, Timed}

/**
 * Created by ivan on 9/5/14.
 */
trait EnvFactoryComponent {

    this : ReadersFactoryComponent with TimeBoundsCalculatorComponent =>

    val envFactory : (ModelConfig=>BacktestEnvironment) = new EnvFactory

    private class EnvFactory extends (ModelConfig=>BacktestEnvironment) {


        override def apply(cfg: ModelConfig): BacktestEnvironment = {
            val bound = timeBoundsCalculator.apply(cfg)
            val readers: Seq[SimpleReader[Timed]] = readersFactory.apply(cfg.tickerConfigs, bound._1)
            class BacktestEnvironmentCtx extends BacktestEnvironmentComponent
            with MarketDataPlayerComponent
            with MarketDataDistributorComponent
            with IntervalServiceComponent{
                override val bounds = bound
                override val tickerPlayers = wrapReadersWithAdapters(readers, cfg.tickerConfigs)
                override val stepMs = cfg.backtestStepInterval.durationMs
            }
            return new BacktestEnvironmentCtx().env

        }

        def appFuncOhlc(lsn: MarketDataListener, idx: Int, curr: Ohlc, next: Ohlc): Unit = lsn.onOhlc(idx, curr, next)

        def appFuncTick(lsn: MarketDataListener, idx: Int, curr: Tick, next: Tick): Unit = lsn.onTick(idx, curr, next)


        def wrapReadersWithAdapters(readers: Seq[SimpleReader[Timed]], tickerCfgs: Seq[TickerConfig]): Seq[ReaderToListenerAdapter] = {
            return readers.zipWithIndex.map(t => {
                val cfg: TickerConfig = tickerCfgs(t._2)
                if (cfg.mdType == MarketDataType.Ohlc)
                    new ReaderToListenerAdapterImpl[Ohlc](t._1.asInstanceOf[SimpleReader[Ohlc]], t._2, appFuncOhlc)
                else
                    new ReaderToListenerAdapterImpl[Tick](t._1.asInstanceOf[SimpleReader[Tick]], t._2, appFuncTick)
            })
        }


    }


}
