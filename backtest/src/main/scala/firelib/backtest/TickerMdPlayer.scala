package firelib.backtest

import java.time.Instant

import firelib.common._

trait TickerMdPlayer {

    def addListener(lsn : IMarketDataListener)
    def readUntil(dtGmt :Instant): Boolean
    def Dispose()
 }
