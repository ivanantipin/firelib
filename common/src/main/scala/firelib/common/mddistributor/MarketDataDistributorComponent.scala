package firelib.common.mddistributor

import java.time.Instant

import firelib.common.core.{ModelConfigContext, OnContextInited}
import firelib.common.interval.{Interval, IntervalServiceComponent}
import firelib.common.misc.{NonDurableTopic, SubTopic, Topic, ohlcUtils, utils}
import firelib.common.timeseries.{TimeSeries, TimeSeriesImpl}
import firelib.domain.{Ohlc, Tick}


trait MarketDataDistributorComponent {


    this : IntervalServiceComponent with ModelConfigContext with OnContextInited =>

    val marketDataDistributor = new MarketDataDistributorImpl()


    class MarketDataDistributorImpl() extends MarketDataDistributor {


        val DEFAULT_TIME_SERIES_HISTORY_LENGTH = 100

        private val timeseries = Array.fill[TimeSeriesContainer](modelConfig.instruments.length)(new TimeSeriesContainer())

        var tickTransformFunction : (Tick) => Tick =_

        def setTickTransformFunction(fun : (Tick) => Tick) : Unit = {
            tickTransformFunction = fun
        }

        var tickListeners : Array[Topic[Tick]] = Array.fill[Topic[Tick]](modelConfig.instruments.length)(new NonDurableTopic[Tick]())
        var ohlcListeners : Array[Topic[Ohlc]] = Array.fill[Topic[Ohlc]](modelConfig.instruments.length)(new NonDurableTopic[Ohlc]())

        setTickTransformFunction(utils.instanceOfClass[Tick=>Tick](modelConfig.tickToTickFuncClass))

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
            val hist = timeseries(idx).getTs(interval)
            hist.adjustSizeIfNeeded(len)
            return hist
        }

        def preInitCurrentBars(time : Instant): Unit ={
            for(cont <- timeseries){
                for(ts <- cont.iterator){
                    ts._2(0).dtGmtEnd=ts._1.ceilTime(time)
                }
            }
        }

        private def createTimeSeries(idx: Int, interval: Interval, len: Int): TimeSeriesImpl[Ohlc] = {
            val lenn = if (len == -1) DEFAULT_TIME_SERIES_HISTORY_LENGTH else len

            val timeSeries = new TimeSeriesImpl[Ohlc](lenn, () => new Ohlc())

            timeseries(idx).addTs(interval, timeSeries)

            intervalService.addListener(interval, (dt) => {
                val next = ohlcUtils.interpolate(timeSeries(0))
                next.dtGmtEnd = dt.plusMillis(interval.durationMs)
                timeSeries.add(next)
            })
            timeSeries
        }

        override def listenTicks(idx : Int, lsn : Tick=>Unit) : Unit = tickListeners(idx).subscribe(lsn)

        override def tickTopic(idx : Int) : SubTopic[Tick] = tickListeners(idx)

        override def listenOhlc(idx : Int, lsn : Ohlc=>Unit) : Unit = ohlcListeners(idx).subscribe(lsn)

        override def getTs(tickerId: Int, interval: Interval): TimeSeries[Ohlc] = timeseries(tickerId).getTs(interval)
    }

}
