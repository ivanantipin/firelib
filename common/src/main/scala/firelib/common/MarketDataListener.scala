package firelib.common

import firelib.domain.{Ohlc, Tick}

/**
 * Created by ivan on 7/21/14.
 */
trait MarketDataListener {
    def onOhlc(idx: Int, ohlc: Ohlc, next: Ohlc)
    def onTick(idx: Int, tick: Tick, next: Tick)

}
