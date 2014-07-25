package firelib.backtest

import firelib.domain.ITimeSeries

import scala.collection.mutable.ArrayBuffer

class TimeSeries[T](val history: HistoryCircular[T]) extends ITimeSeries[T] {

    val lsns = new ArrayBuffer[(ITimeSeries[T]) => Unit]

    def AdjustSizeIfNeeded(historySize: Int) = this.history.AdjustSizeIfNeeded(historySize)

    def Count = history.Count

    def apply(idx: Int): T = history(idx)


    def ShiftAndGetLast: T = {
        lsns.foreach(l => {
            l(this)
        })
        history.ShiftAndGetLast
    }

    override def listen(listener: (ITimeSeries[T]) => Unit): Unit = {
        lsns += listener
    }
}
