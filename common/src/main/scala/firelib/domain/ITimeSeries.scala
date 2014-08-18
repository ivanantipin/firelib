package firelib.common

trait ITimeSeries[T] {
    def adjustSizeIfNeeded(i: Int)

    def count: Int

    def apply(idx: Int): T

    def listen(listener: ITimeSeries[T] => Unit)

    def last : T = apply(0)

}
