package firelib.common

trait ITickProvider {
    def SubscribeForTick(tickerId: Int, act: Tick => Unit)
}
