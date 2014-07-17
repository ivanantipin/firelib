package firelib.robot

import firelib.domain.{Ohlc, Tick}

trait IMarketDataProvider {

    def Subscribe(tickerId: String, lsn: Tick => Unit)

    def Subscribe(tickerId: String, lsn: Ohlc => Unit)
}

