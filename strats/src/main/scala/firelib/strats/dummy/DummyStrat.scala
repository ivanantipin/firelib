package firelib.strats.dummy

import firelib.common._


class DummyStrat extends BasketModel {

    var hists: ITimeSeries[Ohlc] =_


    override protected def applyProperties(mprops: Map[String, String]): Unit = {
        hists = enableOhlcHistory(Interval.Sec10)(0)
        hists.listen(on1Min)
    }

    def on1Min(ts: ITimeSeries[Ohlc]) = {
        System.out.println(ts(0))
        if(ts.count > 2){
            if (position() > 0) {
                managePosTo(-100000)
            } else {
                managePosTo(100000)
            }
        }
    }

    override def onBacktestEnd(): Unit = {
    }

}
