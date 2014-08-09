package firelib.common

import java.time.Instant

import scala.beans.BeanProperty

class Ohlc() extends Timed {

    def this(other: Ohlc) {
        this()
        InitFrom(other)
        DtGmtEnd = other.DtGmtEnd
    }

    private def InitFrom(other: Ohlc) {
        O = other.O
        H = other.H
        L = other.L
        C = other.C
        Volume = other.Volume
        Oi = other.Oi
    }

    private def InitFrom(other: Tick) {
        O = other.Last
        H = other.Last
        L = other.Last
        C = other.Last
        Volume = other.Vol
        Oi = 0
    }

    def Interpolated: Boolean = Volume == 0

    private def AddPrice(last: Double) {
        if (O.isNaN) O = last
        if (H < last) {
            H = last
        }
        if (L > last) {
            L = last
        }
        C = last
    }

    def AddTick(tick: Tick) {
        if (Interpolated) {
            InitFrom(tick)
        }
        AddPrice(tick.Last)
        Volume += tick.Vol
    }

    def AddOhlc(ohlc: Ohlc) {
        if (ohlc.Interpolated) {
            return
        }
        if (Interpolated) {
            InitFrom(ohlc)
        }
        if (O.isNaN) O = ohlc.O
        if (H < ohlc.H) H = ohlc.H
        if (L > ohlc.L) L = ohlc.L
        C = ohlc.C
        Volume += ohlc.Volume
        Oi = ohlc.Oi
    }

    def nprice: Double = (C + H + L) / 3

    def medium: Double = (H + L) / 2

    def IsUpBar: Boolean = C > O

    def Range: Double = H - L

    def UpShadow: Double = H - C

    def DownShadow: Double = C - L

    def BodyLength: Double = Math.abs(C - O)

    def Return: Double = C - O

    def InRange(vv: Double): Boolean = H > vv && vv > L

    override def toString: String = "OHLC(%s/%s/%s/%s@/%s/%s)".format(O, H, L, C, DtGmtEnd.toString, Interpolated)

    def Interpolate(prev: Ohlc) {
        InitFrom(prev)
        Volume = 0
    }

    def DtGmt: Instant = DtGmtEnd

    @BeanProperty
    var C: Double = .0
    @BeanProperty
    var DtGmtEnd: Instant = null
    @BeanProperty
    var H: Double = Integer.MIN_VALUE
    @BeanProperty
    var L: Double = Integer.MAX_VALUE
    @BeanProperty
    var O: Double = Double.NaN
    @BeanProperty
    var Oi: Int = 0
    @BeanProperty
    var Volume: Int = 0
}
