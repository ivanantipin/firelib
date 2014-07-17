package firelib.backtest

import firelib.domain.Trade
import org.joda.time.{DateTime, Duration}

class PositionCloserByTimeOut(val stub: IMarketStub, val holdingTimeSeconds: Int) {

    private var posOpenedDtGmt: DateTime = _
    stub.AddCallback(new TradeGateCallbackAdapter(OnTrade));

    private def OnTrade(trd: Trade) = {
        posOpenedDtGmt = trd.DtGmt;
    }

    def ClosePositionIfTimeOut(dtGmt: DateTime) = {
        val dur = new Duration(posOpenedDtGmt, dtGmt)
        if (stub.Position != 0 && dur.getStandardSeconds > holdingTimeSeconds) {
            stub.FlattenAll();
        }
    }
}
