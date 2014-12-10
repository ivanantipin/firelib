package firelib.execution

import firelib.domain.Tick

trait MarketDataProvider {
    def subscribeForTick(tickerId: String, lsn: Tick => Unit)
}



