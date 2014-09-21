package firelib.common.misc

import firelib.domain.Ohlc


object ohlcUtils{

    def interpolate(from: Ohlc, to: Ohlc) {
        to.O = from.O
        to.H = from.H
        to.L = from.L
        to.C = from.C
        to.Oi = from.Oi
        to.interpolated = true
    }

    def copy(from: Ohlc) : Ohlc ={
        val to: Ohlc = new Ohlc()
        interpolate(from, to)
        to.dtGmtEnd = from.dtGmtEnd
        to.interpolated = from.interpolated
        return to;
    }
}










