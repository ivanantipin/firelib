package firelib.common.mddistributor

import firelib.common.MarketDataListener
import firelib.common.core.BacktestEnvironmentComponent
import firelib.common.interval.{Interval, IntervalServiceComponent}
import firelib.common.mddistributor.MarketDataDistributor
import firelib.common.timeseries.{HistoryCircular, TimeSeries, TimeSeriesImpl}
import firelib.domain.{Ohlc, Tick}

import scala.collection.mutable.ArrayBuffer

/**
 * Created by ivan on 9/5/14.
 */
trait MarketDataDistributorComponent {


    this : IntervalServiceComponent with BacktestEnvironmentComponent =>

    val marketDataDistributor = new MarketDataDistributorImpl()


    class MarketDataDistributorImpl() extends MarketDataDistributor {


        val DEFAULT_TIME_SERIES_HISTORY_LENGTH = 100

        private lazy val tsContainers = Array.fill(tickerPlayers.length) {
            new TsContainer()
        }

        private val listeners = new ArrayBuffer[MarketDataListener]()

        class TsContainer {

            val timeSeries = new ArrayBuffer[(Interval, TimeSeries[Ohlc])]()

            def addOhlc(ohlc: Ohlc) = {
                timeSeries.foreach(_._2(0).addOhlc(ohlc))
            }

            def addTick(tick: Tick) = {
                timeSeries.foreach(_._2(0).addTick(tick))
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


            val ret = new TimeSeriesImpl[Ohlc](new HistoryCircular[Ohlc](lenn, () => new Ohlc()))

            val series = (interval, ret)

            tsContainers(tickerId).timeSeries += series

            intervalService.addListener(interval, (dt) => {
                val prev = ret(0)
                prev.dtGmtEnd = dt
                val last = ret.shiftAndGetLast
                last.interpolateFrom(prev)
                last.dtGmtEnd = dt.plusMillis(interval.durationMs)
            })
            return ret
        }


        override def addMdListener(lsn: MarketDataListener) = listeners += lsn
    }

}
