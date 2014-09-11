package firelib.common.mddistributor

import firelib.common.MarketDataListener
import firelib.common.interval.Interval
import firelib.common.timeseries.TimeSeries
import firelib.domain.Ohlc

/**

 */
trait MarketDataDistributor extends MarketDataListener {
    def activateOhlcTimeSeries(tickerId: Int, interval: Interval, len: Int): TimeSeries[Ohlc]

    def addMdListener(lsn : MarketDataListener)
}
