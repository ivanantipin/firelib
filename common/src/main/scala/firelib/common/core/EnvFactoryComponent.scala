package firelib.common.core

import firelib.common.config.{InstrumentConfig, ModelConfig}
import firelib.common.interval.IntervalServiceComponent
import firelib.common.mddistributor.MarketDataDistributorComponent
import firelib.common.misc.TickToPriceConverterComponent
import firelib.common.reader.{ReaderToListenerAdapter, ReaderToListenerAdapterImpl, ReadersFactoryComponent, SimpleReader}
import firelib.common.timeboundscalc.TimeBoundsCalculatorComponent
import firelib.common.{MarketDataListener, MarketDataType}
import firelib.domain.{Ohlc, Tick, Timed}

/**
 * component of BacktestEnvironment factory for dependency injection
 */

trait ModelConfigContext{
    val modelConfig : ModelConfig
}

trait EnvFactoryComponent {

    this : ReadersFactoryComponent with TimeBoundsCalculatorComponent with ModelConfigContext=>

    val envFactory : (()=>BacktestEnvironment) = new EnvFactory

    private class EnvFactory extends (()=>BacktestEnvironment) {

        override def apply(): BacktestEnvironment = {
            val config = modelConfig
            val bound = timeBoundsCalculator.apply(config)
            val readers: Seq[SimpleReader[Timed]] = readersFactory.apply(modelConfig.instruments, bound._1)
            class BacktestEnvironmentCtx extends BacktestEnvironmentComponent
            with MarketDataPlayerComponent
            with MarketDataDistributorComponent
            with ModelConfigContext
            with TickToPriceConverterComponent
            with IntervalServiceComponent{
                override val modelConfig = config
                override val bounds = bound
                override val tickerPlayers = wrapReadersWithAdapters(readers, config.instruments)
            }
            return new BacktestEnvironmentCtx().env

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
