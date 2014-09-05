package firelib.common.timeseries

/**
 * Created by ivan on 9/5/14.
 */
trait TimeSeries[T] {

    def adjustSizeIfNeeded(i: Int)

    def count: Int

    def apply(idx: Int): T

    def listen(listener: TimeSeries[T] => Unit)

    def last : T = apply(0)

}
