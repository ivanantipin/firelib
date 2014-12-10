package firelib.common.marketstub

import firelib.domain.Ohlc

class OhlcBidAskUpdater(val updatee : {def updateBidAsk(bid: Double, ask: Double)}) extends (Ohlc=>Unit){

    override def apply(oh: Ohlc): Unit = {
        updatee.updateBidAsk(oh.C - 0.005,oh.C + 0.005)
    }
}




