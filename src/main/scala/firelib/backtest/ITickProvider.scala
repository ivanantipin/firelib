package firelib.backtest

import firelib.domain.Tick

trait ITickProvider {
    def SubscribeForTick(tickerId: Int, act: Tick => Unit)
}
