package firelib.common.marketstub

import java.time.Instant

import firelib.common.MarketDataListener
import firelib.common.interval.StepListener
import firelib.domain.{Ohlc, Tick}

class BidAskUpdater(val stub: Seq[BidAskUpdatable]) extends MarketDataListener with StepListener {

    private val bid, ask = new Array[Double](stub.length)

    def onStep(dtGmt:Instant) = {
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
        val tt = if(next == null) tick else next
        if(!tt.bid.isNaN){
            bid(idx) = tt.bid
            ask(idx) = tt.ask
        }else{
            bid(idx) = tt.last
            ask(idx) = tt.last
        }
    }
}


