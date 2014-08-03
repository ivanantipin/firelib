package firelib.backtest

import java.time.Instant

import firelib.common._
class BidAskUpdater(val stub: Seq[IMarketStub]) extends IMarketDataListener with IStepListener {

    private val bid, ask = new Array[Double](stub.length)

    def OnStep(dtGmt:Instant) = {
        for (i <- 0 until stub.length) {
            stub(i).updateBidAskAndTime(bid(i), ask(i), dtGmt)
        }
    }

    override def onOhlc(idx: Int, ohlc: Ohlc, next: Ohlc): Unit = {
        val oh = if(next == null) ohlc else next
        bid(idx) = oh.C - 0.005
        ask(idx) = oh.C + 0.005
    }

    override def onTick(idx: Int, tick: Tick, next: Tick): Unit = {
        val oh = if(next == null) tick else next
        bid(idx) = oh.Bid
        ask(idx) = oh.Ask
    }
}


