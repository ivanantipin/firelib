package firelib.indicators

import firelib.common.ITimeSeries
import firelib.domain.{IIndicator, Ohlc}

class Ema(val period: Int, ts: ITimeSeries[Ohlc],
          val func: Ohlc => Double = (oh)=>oh.C)
  extends IIndicator[Double] with (ITimeSeries[Ohlc] => Unit) {

    var koeffFunc: () => Double = ()=> 2.0 / (period + 1)

    private var ema: Double = 0

    override def value: Double = ema

    override def apply(ts : ITimeSeries[Ohlc]): Unit = {
        val kk = koeffFunc()
        ema = ema * (1 - kk) + func(ts(0)) * kk
    }
}