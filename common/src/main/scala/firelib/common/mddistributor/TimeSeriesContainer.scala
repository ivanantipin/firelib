package firelib.common.mddistributor

import firelib.common.interval.Interval
import firelib.common.misc.{OhlcBuilderFromOhlc, OhlcBuilderFromTick}
import firelib.common.timeseries.TimeSeries
import firelib.domain.{Ohlc, Tick}

import scala.collection.mutable.ArrayBuffer

class TimeSeriesContainer() {

    val timeSeries = new ArrayBuffer[(Interval, TimeSeries[Ohlc])]()

    val ohlcFromTick = new OhlcBuilderFromTick
    val ohlcFromOhlc = new OhlcBuilderFromOhlc

    def addOhlc(ohlc: Ohlc) = {
        timeSeries.foreach(ts=>ohlcFromOhlc.appendOhlc(ts._2(0), ohlc))
    }

    def addTick(tick: Tick) = {
        timeSeries.foreach(ts=>ohlcFromTick.addTick(ts._2(0), tick))
    }

    def contains(interval: Interval) = timeSeries.exists(_._1 == interval)

    def get(interval: Interval): TimeSeries[Ohlc] = timeSeries.find(_._1 == interval).get._2

}
