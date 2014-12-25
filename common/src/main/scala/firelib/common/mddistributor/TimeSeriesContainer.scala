package firelib.common.mddistributor

import firelib.common.interval.Interval
import firelib.common.misc.{OhlcBuilderFromOhlc, OhlcBuilderFromTick}
import firelib.common.timeseries.{TimeSeries, TimeSeriesImpl}
import firelib.domain.{Ohlc, Tick}

import scala.collection.mutable.{ArrayBuffer, HashMap}

class TimeSeriesContainer() {

    private val timeSeries = new ArrayBuffer[TimeSeriesImpl[Ohlc]]()

    private val map = new HashMap[Interval,TimeSeriesImpl[Ohlc]]

    def iterator() : Iterator[(Interval,TimeSeries[Ohlc])] = map.iterator

    val ohlcFromTick = new OhlcBuilderFromTick
    val ohlcFromOhlc = new OhlcBuilderFromOhlc

    def addOhlc(ohlc: Ohlc) = {
        timeSeries.foreach(ts=>ohlcFromOhlc.appendOhlc(ts(0), ohlc))
    }

    def addTick(tick: Tick) = {
        timeSeries.foreach(ts=>ohlcFromTick.addTick(ts(0), tick))
    }

    def addTs(interval : Interval, ts : TimeSeriesImpl[Ohlc]): Unit ={
        map(interval) = ts
        timeSeries += ts
    }

    def contains(interval: Interval) : Boolean = map.contains(interval)

    def getTs(interval: Interval) : TimeSeriesImpl[Ohlc] = map(interval)

}
