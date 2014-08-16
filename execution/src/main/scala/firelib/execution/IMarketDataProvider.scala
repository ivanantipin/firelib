package firelib.execution

import firelib.domain.{Ohlc, Tick}

trait IMarketDataProvider {

    def subscribeForTick(tickerId: String, lsn: Tick => Unit)

    def subscribeForOhlc(tickerId: String, lsn: Ohlc => Unit)
}

