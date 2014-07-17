package firelib.domain

trait ITimeSeries[T] {
    def Count: Int

    def apply(idx: Int): T

    def listen(listener: ITimeSeries[T] => Unit)

}
