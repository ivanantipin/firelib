package firelib.backtest

import org.joda.time.DateTime

/**
 * Created by ivan on 7/21/14.
 */
trait TickerMdPlayer {

    def addListener(lsn : IMarketDataListener)
    def UpdateTimeZoneOffset()
    def ReadUntil(dtGmt : DateTime): Boolean
    def Dispose()
 }
