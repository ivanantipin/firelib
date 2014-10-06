package firelib.common.misc

import firelib.domain.{Ohlc, Tick}

class OhlcBuilderFromTick() {
    def addTick(currOhlc : Ohlc, tick: Tick) {
        val price: Double = tick.last
        if (currOhlc.interpolated) {
            currOhlc.O = price
            currOhlc.H = price
            currOhlc.L = price
            currOhlc.C = price
            currOhlc.Volume = tick.vol
            currOhlc.Oi = 0
            currOhlc.interpolated = false
        }else{
            currOhlc.H = math.max(price, currOhlc.H)
            currOhlc.L = math.min(price, currOhlc.L)
            currOhlc.C = price
            currOhlc.Volume += tick.vol
        }
    }

}

class OhlcBuilderFromOhlc() {
    def appendOhlc(currOhlc : Ohlc, ohlc: Ohlc) {
        if (currOhlc.interpolated) {
            ohlcUtils.interpolate(ohlc,currOhlc)
            currOhlc.interpolated = false
        }else{
            currOhlc.H = math.max(ohlc.H, currOhlc.H)
            currOhlc.L = math.min(ohlc.L, currOhlc.L)
            currOhlc.C = ohlc.C
            currOhlc.Volume += ohlc.Volume
            currOhlc.Oi += ohlc.Oi
        }
    }
}

