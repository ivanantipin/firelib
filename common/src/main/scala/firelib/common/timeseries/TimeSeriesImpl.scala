package firelib.common.timeseries

import scala.collection.mutable.ArrayBuffer

class TimeSeriesImpl[T](val history: HistoryCircular[T]) extends TimeSeries[T] {

    private val listeners = new ArrayBuffer[(TimeSeries[T]) => Unit]

    def adjustSizeIfNeeded(historySize: Int) = this.history.adjustSizeIfNeeded(historySize)

    def count = history.count

    def apply(idx: Int): T = history(idx)

    def shiftAndGetLast: T = {
        listeners.foreach(_(this))
        history.shift
    }

    override def listen(listener: (TimeSeries[T]) => Unit): Unit = {
        listeners += listener
    }
}
