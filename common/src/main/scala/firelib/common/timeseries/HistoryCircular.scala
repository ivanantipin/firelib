package firelib.common.timeseries

import scala.reflect.ClassTag

/*
 * this is simple window - convention for index is
 * idx == 0 - current value
 * idx < 0 value of idx "intervals ago"
 */
class HistoryCircular[T:ClassTag](val length: Int, func: () => T) {

    var data = new Array[T](0)
    var count = 0
    var head = 0
    adjustSizeIfNeeded(length)

    def apply(idx: Int): T = data(calcIdx(idx))

    def calcIdx(idx: Int): Int = (head + idx + length) % length

    def update(idx : Int, value : T ) = data(calcIdx(idx)) = value

    def shift: T = {
        count+=1
        head = (head + 1) % length
        data(head)
    }

    def adjustSizeIfNeeded(historySize: Int): Unit = {
        if (historySize > data.length) {
            data = Array.fill[T](length) {func()}
        }
    }
}
