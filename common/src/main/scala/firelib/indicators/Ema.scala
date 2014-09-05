package firelib.indicators

import firelib.common.timeseries.TimeSeries
import firelib.domain.Ohlc

class Ema(
          val period: Int,
          ts: TimeSeries[Ohlc],
          val func: Ohlc => Double = (oh)=>oh.C)
  extends Indicator[Double] with (TimeSeries[Ohlc] => Unit) {

    var koeffFunc: () => Double = ()=> 2.0 / (period + 1)

    ts.listen(this)

    private var ema: Double = 0

    override def value: Double = ema

    override def apply(ts : TimeSeries[Ohlc]): Unit = {
        val kk = koeffFunc()
        ema = ema * (1 - kk) + func(ts(0)) * kk
    }
}