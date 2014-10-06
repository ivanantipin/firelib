package firelib.common.mddistributor

import firelib.common.MarketDataListener
import firelib.common.core.{ModelConfigContext, OnContextInited}
import firelib.common.interval.{Interval, IntervalServiceComponent}
import firelib.common.misc.{OhlcBuilderFromOhlc, OhlcBuilderFromTick, ohlcUtils, utils}
import firelib.common.timeseries.{HistoryCircular, TimeSeries, TimeSeriesImpl}
import firelib.domain.{Ohlc, Tick}

import scala.collection.mutable.ArrayBuffer

/**

 */
trait MarketDataDistributorComponent {


    this : IntervalServiceComponent with ModelConfigContext with OnContextInited =>

    val marketDataDistributor = new MarketDataDistributorImpl()


    class MarketDataDistributorImpl() extends MarketDataDistributor {


        val DEFAULT_TIME_SERIES_HISTORY_LENGTH = 100

        private var instrumentToTimeSeries : Seq[TsContainer] =_;

        initMethods += init

        var tickToTick : (Tick) => Tick =_

        def setTickToTickFunc(fun : (Tick) => Tick) : Unit = {
            tickToTick = fun
        }

        def init(): Unit ={
            instrumentToTimeSeries = modelConfig.instruments.map(inst=>{
                new TsContainer()
            })
            tickToTick = utils.instanceOfClass[Tick=>Tick](modelConfig.tickToTickFuncClass)
        }



        private val listeners = new ArrayBuffer[MarketDataListener]()

        class TsContainer() {

            val timeSeries = new ArrayBuffer[(Interval, TimeSeries[Ohlc])]()

            val ohlcFromTick = new OhlcBuilderFromTick
            val ohlcFromOhlc = new OhlcBuilderFromOhlc

            def addOhlc(ohlc: Ohlc) = {
                timeSeries.foreach(ts=>ohlcFromOhlc.appendOhlc(ts._2(0), ohlc))
            }

            def addTick(tick: Tick) = {
                timeSeries.foreach(ts=>ohlcFromTick.addTick(ts._2(0), tick))
            }

            def hasTsForInterval(interval: Interval) = timeSeries.exists(_._1 == interval)

            def getTsForInterval(interval: Interval): TimeSeries[Ohlc] = timeSeries.find(_._1 == interval).get._2

        }

        override def onOhlc(idx: Int, ohlc: Ohlc, next: Ohlc): Unit = {
            instrumentToTimeSeries(idx).addOhlc(ohlc)
            listeners.foreach(_.onOhlc(idx, ohlc, next))
        }

        override def onTick(idx: Int, tick: Tick, next: Tick): Unit = {
            val ntick = tickToTick(tick)
            val nnexttick = if(next == null) null else tickToTick(next)
            instrumentToTimeSeries(idx).addTick(ntick)
            listeners.foreach(_.onTick(idx, ntick, nnexttick))
        }


        def activateOhlcTimeSeries(idx: Int, interval: Interval, len: Int): TimeSeries[Ohlc] = {
            if (!instrumentToTimeSeries(idx).hasTsForInterval(interval)) {
                createTimeSeries(idx, interval, len)
            }
            var hist = instrumentToTimeSeries(idx).getTsForInterval(interval)
            hist.adjustSizeIfNeeded(len)
            return hist
        }

        private def createTimeSeries(tickerId: Int, interval: Interval, len: Int): TimeSeriesImpl[Ohlc] = {
            val lenn = if (len == -1) DEFAULT_TIME_SERIES_HISTORY_LENGTH else len


            val timeSeries = new TimeSeriesImpl[Ohlc](new HistoryCircular[Ohlc](lenn, () => new Ohlc()))

            val series = (interval, timeSeries)

            instrumentToTimeSeries(tickerId).timeSeries += series

            intervalService.addListener(interval, (dt) => {
                val prev = timeSeries(0)
                prev.dtGmtEnd = dt
                val last = timeSeries.shiftAndGetLast
                ohlcUtils.interpolate(prev,last)
                last.dtGmtEnd = dt.plusMillis(interval.durationMs)
            })
            return timeSeries
        }


        override def addMdListener(lsn: MarketDataListener) = listeners += lsn
    }

}
