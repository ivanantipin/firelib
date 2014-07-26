package firelib.common

trait ITimeSeries[T] {
    def AdjustSizeIfNeeded(i: Int)

    def Count: Int

    def apply(idx: Int): T

    def listen(listener: ITimeSeries[T] => Unit)

}
