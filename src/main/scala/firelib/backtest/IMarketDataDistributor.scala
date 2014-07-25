package firelib.backtest

import firelib.domain.{ITimeSeries, Interval, Ohlc}


trait IMarketDataDistributor {
    def activateOhlcTimeSeries(tickerId: Int, interval: Interval, len: Int): ITimeSeries[Ohlc]

    def addMdListener(lsn : IMarketDataListener)
}
