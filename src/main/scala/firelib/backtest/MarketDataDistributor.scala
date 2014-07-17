package firelib.backtest


import firelib.domain._

import scala.collection.mutable.{HashMap, ListBuffer}


class MarketDataDistributor(tickerConfigs: List[TickerConfig], val intervalService: IIntervalService) extends IMarketDataDistributor with IQuoteListener[Ohlc] with IQuoteListener[Tick] {


    val tickerUnits = tickerConfigs.map(tc => new TickerUnit())

    val series = new Array[HashMap[Interval, TimeSeries[Ohlc]]](tickerConfigs.length)


    for (i <- 0 until series.length) {
        series(i) = new HashMap[Interval, TimeSeries[Ohlc]]();
    }

    val DEFAULT_TIME_SERIES_HISTORY_LENGTH = 100;

    class TickerUnit {

        val quoteListeners = new ListBuffer[Ohlc => Unit]()

        val timeSeries = new ListBuffer[ITimeSeries[Ohlc]]()

        val tickSubscribers = new ListBuffer[Tick => Unit]()

        def AddOhlc(ohlc: Ohlc) = {
            timeSeries.foreach(ts => ts(0).AddOhlc(ohlc))
        }

        def AddTick(tick: Tick) = {
            timeSeries.foreach(ts => ts(0).AddTick(tick))

            tickSubscribers.foreach(ts => ts(tick))

        }


        def AddTickSubscriber(subsc: Tick => Unit) = {
            tickSubscribers += subsc
        }


        def AddTimeSerie(timeSerie: ITimeSeries[Ohlc]) = {
            timeSeries += timeSerie
        }

    }


    def AddQuote(idx: Int, quote: Ohlc) {
        tickerUnits(idx).AddOhlc(quote);
    }

    def AddQuote(idx: Int, quote: Tick) = {
        tickerUnits(idx).AddTick(quote);
    }


    def SubscribeForTick(tickerId: Int, subscr: Tick => Unit) = {
        tickerUnits(tickerId).AddTickSubscriber(subscr);
    }

    def activateOhlcTimeSeries(tickerId: Int, interval: Interval, len: Int): ITimeSeries[Ohlc] = {
        if (!series(tickerId).contains(interval)) {
            series(tickerId)(interval) = CreateTimeSerie(tickerId, interval, len)
        }
        var hist = series(tickerId)(interval)
        hist.AdjustSizeIfNeeded(len);
        return hist;
    }

    def CreateTimeSerie(tickerId: Int, interval: Interval, len: Int): TimeSeries[Ohlc] = {
        val lenn = if (len == -1) DEFAULT_TIME_SERIES_HISTORY_LENGTH else len

        var ret = new TimeSeries[Ohlc](new HistoryCircular[Ohlc](len, () => new Ohlc()));

        tickerUnits(tickerId).AddTimeSerie(ret);

        intervalService.AddListener(interval, (dt) => {
            var prev = ret(0)
            prev.DtGmtEnd = dt;
            var last = ret.ShiftAndGetLast
            last.Interpolate(prev);
            last.DtGmtEnd = dt.plusMillis(interval.durationMs)
        });
        return ret;
    }
}
