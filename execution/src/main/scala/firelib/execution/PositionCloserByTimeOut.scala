package firelib.backtest

import java.time.Instant

import firelib.common._


class PositionCloserByTimeOut(val stub: IMarketStub, val holdingTimeSeconds: Int) {

    private var posOpenedDtGmt: Instant  = _
    stub.addCallback(new TradeGateCallbackAdapter(OnTrade));

    private def OnTrade(trd: Trade) = {
        posOpenedDtGmt = trd.DtGmt;
    }

    def ClosePositionIfTimeOut(dtGmt: Instant) = {
        if (stub.Position != 0 && dtGmt.getEpochSecond - posOpenedDtGmt.getEpochSecond > holdingTimeSeconds) {
            stub.flattenAll();
        }
    }
}
