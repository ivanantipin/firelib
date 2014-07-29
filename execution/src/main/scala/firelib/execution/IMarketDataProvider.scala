package firelib.robot

import firelib.common._

trait IMarketDataProvider {

    def SubscribeForTick(tickerId: String, lsn: Tick => Unit)

    def SubscribeForOhlc(tickerId: String, lsn: Ohlc => Unit)
}

