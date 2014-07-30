package firelib.robot

import firelib.common._

trait IMarketDataProvider {

    def subscribeForTick(tickerId: String, lsn: Tick => Unit)

    def subscribeForOhlc(tickerId: String, lsn: Ohlc => Unit)
}

