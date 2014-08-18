package firelib.indicators

import firelib.common.ITimeSeries
import firelib.domain.{IIndicator, Ohlc}

class ATR(period: Int, ts: ITimeSeries[Ohlc]) extends IIndicator[Double] with (ITimeSeries[Ohlc] => Unit) {

    override def value: Double = avg.value

    val avg = new SimpleMovingAverage(period, false)

    ts.listen(this)

    override def apply(v1: ITimeSeries[Ohlc]): Unit = {
        avg.add(lastRange(v1))
    }

    private def lastRange(ts: ITimeSeries[Ohlc]): Double = {
        val o = ts(0)
        if (o.interpolated) {
            return Double.NaN;
        }
        if (ts.count == 1)
            return o.H - o.L;
        return math.max(o.H - o.L, math.max(o.H - ts(-1).C, ts(-1).C - o.H));
    }

}
