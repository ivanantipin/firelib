package firelib.common.mddistributor

import firelib.common.MarketDataListener
import firelib.common.core.ModelConfigContext
import firelib.common.interval.{Interval, IntervalServiceComponent}
import firelib.common.misc.{OhlcBuilderFromTick, TickToPriceConverterComponent, ohlcUtils}
import firelib.common.timeseries.{HistoryCircular, TimeSeries, TimeSeriesImpl}
import firelib.domain.{Ohlc, Tick}

import scala.collection.mutable.ArrayBuffer

/**

 */
trait MarketDataDistributorComponent {


    this : IntervalServiceComponent with TickToPriceConverterComponent with ModelConfigContext=>

    val marketDataDistributor = new MarketDataDistributorImpl()


    class MarketDataDistributorImpl() extends MarketDataDistributor {


        val DEFAULT_TIME_SERIES_HISTORY_LENGTH = 100

        private lazy val tsContainers = modelConfig.instruments.map(inst=>{
            new TsContainer(new OhlcBuilderFromTick(tickToPriceConverterFactory(inst)))
        });

        def appendOhlc(currOhlc : Ohlc, ohlc: Ohlc) {
            if (currOhlc.interpolated) {
                ohlcUtils.interpolate(ohlc,currOhlc)
                currOhlc.interpolated = false
            }else{
                currOhlc.H = math.max(ohlc.H, currOhlc.H)
                currOhlc.L = math.min(ohlc.L, currOhlc.L)
                currOhlc.C = ohlc.C
                currOhlc.Volume += ohlc.Volume
                currOhlc.Oi += ohlc.Oi
            }
        }



        private val listeners = new ArrayBuffer[MarketDataListener]()

        class TsContainer(val ohlcBuilder : OhlcBuilderFromTick) {

            val timeSeries = new ArrayBuffer[(Interval, TimeSeries[Ohlc])]()

            def addOhlc(ohlc: Ohlc) = {
                timeSeries.foreach(ts=>appendOhlc(ts._2(0), ohlc))
            }

            def addTick(tick: Tick) = {
                timeSeries.foreach(ts=>ohlcBuilder.addTick(ts._2(0), tick))
            }

            def hasTsForInterval(interval: Interval) = timeSeries.exists(_._1 == interval)

            def getTsForInterval(interval: Interval): TimeSeries[Ohlc] = timeSeries.find(_._1 == interval).get._2

        }

        override def onOhlc(idx: Int, ohlc: Ohlc, next: Ohlc): Unit = {
            tsContainers(idx).addOhlc(ohlc)
            listeners.foreach(_.onOhlc(idx, ohlc, next))
        }

        override def onTick(idx: Int, tick: Tick, next: Tick): Unit = {
            tsContainers(idx).addTick(tick)
            listeners.foreach(_.onTick(idx, tick, next))
        }


        def activateOhlcTimeSeries(idx: Int, interval: Interval, len: Int): TimeSeries[Ohlc] = {
            if (!tsContainers(idx).hasTsForInterval(interval)) {
                createTimeSeries(idx, interval, len)
            }
            var hist = tsContainers(idx).getTsForInterval(interval)
            hist.adjustSizeIfNeeded(len)
            return hist
        }

        private def createTimeSeries(tickerId: Int, interval: Interval, len: Int): TimeSeriesImpl[Ohlc] = {
            val lenn = if (len == -1) DEFAULT_TIME_SERIES_HISTORY_LENGTH else len


            val timeSeries = new TimeSeriesImpl[Ohlc](new HistoryCircular[Ohlc](lenn, () => new Ohlc()))

            val series = (interval, timeSeries)

            tsContainers(tickerId).timeSeries += series

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
