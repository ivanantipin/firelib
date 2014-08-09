package firelib.common

import scala.reflect.ClassTag

/*
 * this is simple window - convention for index is
 * idx == 0 - current value
 * idx < 0 value of idx "intervals ago"
 */
class HistoryCircular[T:ClassTag](val length: Int, func: () => T) {

    var data = Array.fill[T](length) {
        func()
    }
    var count = 0
    var head = 0

    def apply(idx: Int): T = data((head + idx + length) % length)

    def shiftAndGetLast: T = {
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
