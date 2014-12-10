package firelib.common.mddistributor

import firelib.common.interval.Interval
import firelib.common.timeseries.TimeSeries
import firelib.domain.{Ohlc, Tick}

/**

 */
trait MarketDataDistributor {

    def activateOhlcTimeSeries(tickerId: Int, interval: Interval, len: Int): TimeSeries[Ohlc]

    def listenTicks(idx : Int, lsn : Tick=>Unit) : Unit

    def listenOhlc(idx : Int, lsn : Ohlc=>Unit) : Unit

    def setTickTransformFunction(fun : (Tick) => Tick) : Unit
}
