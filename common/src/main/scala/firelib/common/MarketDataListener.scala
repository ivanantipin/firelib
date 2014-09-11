package firelib.common

import firelib.domain.{Ohlc, Tick}

/**

 */
trait MarketDataListener {
    def onOhlc(idx: Int, ohlc: Ohlc, next: Ohlc)
    def onTick(idx: Int, tick: Tick, next: Tick)

}
