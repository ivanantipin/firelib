package firelib.strats.dummy

import firelib.common.{BasketModel, ITimeSeries, Interval, Ohlc}


class DummyStrat extends BasketModel {

    var hists: ITimeSeries[Ohlc] =_


    override protected def applyProperties(mprops: Map[String, String]): Unit = {
        hists = enableOhlcHistory(Interval.Day)(0)
        hists.listen(on1Min)
    }

    def on1Min(ts: ITimeSeries[Ohlc]) = {
        if(ts.count > 2){
            if (position() > 0) {
                managePosTo(-1)
            } else {
                managePosTo(1)
            }
        }
    }

    override def onBacktestEnd(): Unit = {
        System.out.print()
    }
}
