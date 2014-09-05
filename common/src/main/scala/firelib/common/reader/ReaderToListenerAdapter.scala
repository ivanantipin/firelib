package firelib.common.reader

import java.time.Instant

import firelib.common.MarketDataListener

/**
 * Created by ivan on 9/5/14.
 */
trait ReaderToListenerAdapter {

    def addListener(lsn : MarketDataListener)
    def readUntil(dtGmt :Instant): Boolean
    def close()
 }
