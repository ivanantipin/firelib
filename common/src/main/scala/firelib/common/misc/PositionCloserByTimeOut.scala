package firelib.common.misc

import java.time.Instant

import firelib.common._
import firelib.common.ordermanager.OrderManager
import firelib.common.timeseries.TimeSeries
import firelib.domain.Ohlc

class PositionCloserByTimeOut(val stub: OrderManager, val holdingTimeSeconds: Int, val ts : TimeSeries[Ohlc] = null) {

    private var posOpenedDtGmt: Instant  = _
    stub.listenTrades(onTrade)

    if(ts != null){
        ts.listen(tt => closePositionIfTimeOut(ts.last.dtGmtEnd))
    }

    private def onTrade(trd: Trade) = {
        posOpenedDtGmt = trd.dtGmt
    }

    def closePositionIfTimeOut(dtGmt: Instant) = {
        if (stub.position != 0 && dtGmt.getEpochSecond - posOpenedDtGmt.getEpochSecond > holdingTimeSeconds) {
            stub.managePosTo(0)
        }
    }
}
