package firelib.backtest

import java.time.Instant

import firelib.common._

trait TickerMdPlayer {

    def addListener(lsn : IMarketDataListener)
    def UpdateTimeZoneOffset()
    def ReadUntil(dtGmt :Instant): Boolean
    def Dispose()
 }
