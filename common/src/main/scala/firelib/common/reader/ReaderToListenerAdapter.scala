package firelib.common.reader

import java.time.Instant

import firelib.common.MarketDataListener

/**

 */
trait ReaderToListenerAdapter {
    def addListener(lsn : MarketDataListener)
    def readUntil(dtGmt :Instant): Boolean
    def close()
 }
