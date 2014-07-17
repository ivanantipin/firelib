package firelib.backtest

import firelib.domain.{ITimeSeries, Interval, Ohlc}


trait IMarketDataDistributor extends ITickProvider {
    def activateOhlcTimeSeries(tickerId: Int, interval: Interval, len: Int): ITimeSeries[Ohlc]
}
