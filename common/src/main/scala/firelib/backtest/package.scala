package firelib

import java.time.Instant

import firelib.common._
import firelib.domain.Timed

import scala.collection.Map

package object backtest {
    type ReadersFactory = (Seq[TickerConfig], Instant) => Seq[ISimpleReader[Timed]]

    type MarketStubFactory = TickerConfig => IMarketStub

    type TimeBoundsCalculator = ModelConfig=>(Instant,Instant)

    type MetricsCalculator = (Seq[(Trade, Trade)]) => Map[StrategyMetric, Double]

    type BacktestEnvironmentFactory = ModelConfig=>BacktestEnvironment


    object defaultMarketStubFactory extends MarketStubFactory{
        override def apply(v1: TickerConfig): IMarketStub = new MarketStub(v1.ticker)
    }

    object dummyReaderFactory extends ReadersFactory{
        override def apply(cfgs: Seq[TickerConfig], startTime: Instant): Seq[ISimpleReader[Timed]] =  cfgs.map(_=>dummyReader)
    }

    object dummyTimeBoundsCalculator extends TimeBoundsCalculator{
        override def apply(cfg: ModelConfig): (Instant, Instant) = (Instant.MAX,Instant.MIN)
    }


    /**
     * dummy reader used when no backtest required just model initialization
     */
    val dummyReader: ISimpleReader[Timed] = new ISimpleReader[Timed] {

        override def seek(time: Instant): Boolean = true

        override def endTime(): Instant = Instant.MAX

        override def read(): Boolean = false

        override def startTime(): Instant = Instant.MAX

        override def current: Timed = null

        override def close(): Unit = {}
    }

}

