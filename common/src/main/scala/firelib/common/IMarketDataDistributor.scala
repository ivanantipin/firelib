package firelib.common

import firelib.domain.Ohlc


trait IMarketDataDistributor {
    def activateOhlcTimeSeries(tickerId: Int, interval: Interval, len: Int): ITimeSeries[Ohlc]

    def addMdListener(lsn : IMarketDataListener)
}