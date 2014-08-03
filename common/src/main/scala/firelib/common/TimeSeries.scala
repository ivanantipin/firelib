package firelib.common

import scala.collection.mutable.ArrayBuffer

class TimeSeries[T](val history: HistoryCircular[T]) extends ITimeSeries[T] {

    private val listeners = new ArrayBuffer[(ITimeSeries[T]) => Unit]

    def adjustSizeIfNeeded(historySize: Int) = this.history.adjustSizeIfNeeded(historySize)

    def count = history.count

    def apply(idx: Int): T = history(idx)

    def shiftAndGetLast: T = {
        listeners.foreach(_(this))
        history.shiftAndGetLast
    }

    override def listen(listener: (ITimeSeries[T]) => Unit): Unit = {
        listeners += listener
    }
}
