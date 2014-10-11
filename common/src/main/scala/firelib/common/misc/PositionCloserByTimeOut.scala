package firelib.common.misc

import java.time.Instant

import firelib.common.{TradeGateCallbackAdapter, _}
import firelib.common.marketstub.OrderManager
import firelib.common.model.withTradeUtils.WithTradeUtils
class PositionCloserByTimeOut(val stub: OrderManager, val holdingTimeSeconds: Int) {

    private var posOpenedDtGmt: Instant  = _
    stub.addCallback(new TradeGateCallbackAdapter(onTrade))

    private def onTrade(trd: Trade) = {
        posOpenedDtGmt = trd.dtGmt
    }

    def closePositionIfTimeOut(dtGmt: Instant) = {
        if (stub.position != 0 && dtGmt.getEpochSecond - posOpenedDtGmt.getEpochSecond > holdingTimeSeconds) {
            stub.managePosTo(0)
        }
    }
}
