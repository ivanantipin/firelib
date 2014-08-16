package firelib.backtest

import java.time.Instant

import firelib.common._


class PositionCloserByTimeOut(val stub: IMarketStub, val holdingTimeSeconds: Int) {

    private var posOpenedDtGmt: Instant  = _
    stub.addCallback(new TradeGateCallbackAdapter(onTrade))

    private def onTrade(trd: Trade) = {
        posOpenedDtGmt = trd.dtGmt
    }

    def closePositionIfTimeOut(dtGmt: Instant) = {
        if (stub.position != 0 && dtGmt.getEpochSecond - posOpenedDtGmt.getEpochSecond > holdingTimeSeconds) {
            stub.flattenAll(None)
        }
    }
}
