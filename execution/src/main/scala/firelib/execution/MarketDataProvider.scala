package firelib.execution

import firelib.common.threading.ThreadExecutor
import firelib.domain.{Ohlc, Tick}

trait MarketDataProvider {

    def subscribeForTick(tickerId: String, lsn: Tick => Unit)

    def subscribeForOhlc(tickerId: String, lsn: Ohlc => Unit)

    /**
     * pass configuration params to gate
     * usually it is user/password, broker port and url etc
     */
    def configure(config: Map[String, String], callbackExecutor: ThreadExecutor)

    /**
     * need to run start to finish initialization
     */
    def start()

}

