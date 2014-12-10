package firelib.common.mddistributor

import firelib.common.core.{ModelConfigContext, OnContextInited}
import firelib.common.interval.{Interval, IntervalServiceComponent}
import firelib.common.misc.{NonDurableTopic, Topic, ohlcUtils, utils}
import firelib.common.timeseries.{HistoryCircular, TimeSeries, TimeSeriesImpl}
import firelib.domain.{Ohlc, Tick}


trait MarketDataDistributorComponent {


    this : IntervalServiceComponent with ModelConfigContext with OnContextInited =>

    val marketDataDistributor = new MarketDataDistributorImpl()


    class MarketDataDistributorImpl() extends MarketDataDistributor {


        val DEFAULT_TIME_SERIES_HISTORY_LENGTH = 100

        private var timeseries : Array[TimeSeriesContainer] =_;

        var tickTransformFunction : (Tick) => Tick =_

        def setTickTransformFunction(fun : (Tick) => Tick) : Unit = {
            tickTransformFunction = fun
        }

        var tickListeners : Array[Topic[Tick]] =_

        var ohlcListeners : Array[Topic[Ohlc]] =_

        timeseries = Array.fill[TimeSeriesContainer](modelConfig.instruments.length)(new TimeSeriesContainer())
        tickListeners = Array.fill[Topic[Tick]](modelConfig.instruments.length)(new NonDurableTopic[Tick]())
        ohlcListeners = Array.fill[Topic[Ohlc]](modelConfig.instruments.length)(new NonDurableTopic[Ohlc]())
        tickTransformFunction = utils.instanceOfClass[Tick=>Tick](modelConfig.tickToTickFuncClass)

        def onOhlc(idx: Int, ohlc: Ohlc): Unit = {
            ohlcListeners(idx).publish(ohlc)
            timeseries(idx).addOhlc(ohlc)
        }

        def onTick(idx: Int, tick: Tick): Unit = {
            val ntick = tickTransformFunction(tick)
            timeseries(idx).addTick(ntick)
            tickListeners(idx).publish(ntick)
        }


        def activateOhlcTimeSeries(idx: Int, interval: Interval, len: Int): TimeSeries[Ohlc] = {
            if (!timeseries(idx).contains(interval)) {
                createTimeSeries(idx, interval, len)
            }
            var hist = timeseries(idx).get(interval)
            hist.adjustSizeIfNeeded(len)
            return hist
        }

        private def createTimeSeries(idx: Int, interval: Interval, len: Int): TimeSeriesImpl[Ohlc] = {
            val lenn = if (len == -1) DEFAULT_TIME_SERIES_HISTORY_LENGTH else len

            val timeSeries = new TimeSeriesImpl[Ohlc](new HistoryCircular[Ohlc](lenn, () => new Ohlc()))

            timeseries(idx).timeSeries += ((interval, timeSeries))

            intervalService.addListener(interval, (dt) => {
                val prev = timeSeries(0)
                prev.dtGmtEnd = dt
                val last = timeSeries.shiftAndGetLast
                ohlcUtils.interpolate(prev,last)
                last.dtGmtEnd = dt.plusMillis(interval.durationMs)
            })
            return timeSeries
        }

        override def listenTicks(idx : Int, lsn : Tick=>Unit) : Unit = tickListeners(idx).subscribe(lsn)

        override def listenOhlc(idx : Int, lsn : Ohlc=>Unit) : Unit = ohlcListeners(idx).subscribe(lsn)

    }

}
