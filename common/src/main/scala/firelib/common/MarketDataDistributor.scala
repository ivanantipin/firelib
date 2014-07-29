package firelib.common


import scala.collection.mutable.ArrayBuffer


class MarketDataDistributor(length : Int, val intervalService: IIntervalService) extends IMarketDataDistributor with IMarketDataListener {


    val DEFAULT_TIME_SERIES_HISTORY_LENGTH = 100

    val tsContainers = Array.fill(length){new TsContainer()}

    val listeners = new ArrayBuffer[IMarketDataListener]()

    class TsContainer {

        val timeSeries = new ArrayBuffer[(Interval,ITimeSeries[Ohlc])]()

        def AddOhlc(ohlc: Ohlc) = {
            timeSeries.foreach(_._2(0).AddOhlc(ohlc))
        }

        def AddTick(tick: Tick) = {
            timeSeries.foreach(_._2(0).AddTick(tick))
        }


        def hasTsForInterval(interval : Interval) = timeSeries.exists(_._1 == interval)

        def getTsForInterval(interval : Interval) : ITimeSeries[Ohlc] = timeSeries.find(_._1 == interval).get._2

    }

    override def onOhlc(idx: Int, ohlc: Ohlc, next: Ohlc): Unit = {
        tsContainers(idx).AddOhlc(ohlc);
        listeners.foreach(_.onOhlc(idx,ohlc,next))
    }

    override def onTick(idx: Int, tick: Tick, next: Tick): Unit = {
        tsContainers(idx).AddTick(tick);
        listeners.foreach(_.onTick(idx,tick,next))
    }


    def activateOhlcTimeSeries(idx: Int, interval: Interval, len: Int): ITimeSeries[Ohlc] = {
        if (!tsContainers(idx).hasTsForInterval(interval)) {
            CreateTimeSerie(idx, interval, len)
        }
        var hist = tsContainers(idx).getTsForInterval(interval)
        hist.AdjustSizeIfNeeded(len);
        return hist;
    }

    private def CreateTimeSerie(tickerId: Int, interval: Interval, len: Int): TimeSeries[Ohlc] = {
        val lenn = if (len == -1) DEFAULT_TIME_SERIES_HISTORY_LENGTH else len


        var ret = new TimeSeries[Ohlc](new HistoryCircular[Ohlc](len, () => new Ohlc()));

        val series = (interval, ret)

        tsContainers(tickerId).timeSeries += series;

        intervalService.AddListener(interval, (dt) => {
            var prev = ret(0)
            prev.DtGmtEnd = dt;
            var last = ret.ShiftAndGetLast
            last.Interpolate(prev);
            last.DtGmtEnd = dt.plusMillis(interval.durationMs)
        });
        return ret;
    }



    override def addMdListener(lsn: IMarketDataListener) = listeners += lsn
}
