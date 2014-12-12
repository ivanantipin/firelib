package firelib.indicators
import firelib.common.timeseries.TimeSeries
import firelib.domain.Ohlc
class Ma(val period: Int,
          ts: TimeSeries[Ohlc])
  extends Indicator[Double] with (TimeSeries[Ohlc] => Unit) {

    ts.listen(this)

    val maa = new SimpleMovingAverage(period, false)

    override def value: Double = maa.value

    override def apply(ts: TimeSeries[Ohlc]): Unit = {
        if (!ts(0).interpolated) {
            maa.add(ts(0).C)
        }
    }
}
