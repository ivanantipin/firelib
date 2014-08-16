package firelib.backtest

import java.time.Instant

import firelib.common._

trait ReaderToListenerAdapter {

    def addListener(lsn : IMarketDataListener)
    def readUntil(dtGmt :Instant): Boolean
    def close()
 }
