package firelib.indicators

import firelib.common.timeseries.TimeSeries
import firelib.domain.Ohlc

class Ema(
          val period: Int,
          ts: TimeSeries[Ohlc],
          val func: Ohlc => Double = (oh)=>oh.C)
  extends Indicator[Double] with (TimeSeries[Ohlc] => Unit) {

    var koeffFunc = ()=> 2.0 / (period + 1)

    ts.listen(this)

    private var ema: Double = 0

    override def value: Double = ema

    override def apply(ts : TimeSeries[Ohlc]): Unit = {
        if(!ts(0).interpolated){
            val kk = koeffFunc()
            ema = ema * (1 - kk) + func(ts(0)) * kk
        }
    }
}

class SimpleEma(val period: Int) {
    var koeffFunc = () => 2.0 / (period + 1)
    var ema: Double = 0
    def add(va: Double): Unit = {
        val kk = koeffFunc()
        ema = ema * (1 - kk) + va * kk
    }
}


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
