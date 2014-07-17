package firelib.backtest

/*
 * this is simple window - convention for index is
 * idx == 0 - current value
 * idx < 0 value of idx "intervals ago"
 */
class HistoryCircular[T](val length: Int, func: () => T) {

    var data = Array.fill[T](length) {
        func()
    }
    var Count = 0;
    var head = 0

    def apply(idx: Int): T = {
        val cidx: Int = (head + idx + length) % length
        data(cidx)
    }

    def ShiftAndGetLast: T = {
        head = (head + 1) % length
        return data(head);
    }

    def AdjustSizeIfNeeded(historySize: Int) {
        if (historySize < data.length) {
            return;
        }
        data = Array.fill[T](length) {
            func()
        }
    }
}
