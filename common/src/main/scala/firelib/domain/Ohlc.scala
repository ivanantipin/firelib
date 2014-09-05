package firelib.domain

import java.time.Instant

import firelib.common.misc.dateUtils
import dateUtils._

import scala.beans.BeanProperty

class Ohlc() extends Timed {

    def this(other: Ohlc) {
        this()
        initFrom(other)
        dtGmtEnd = other.dtGmtEnd
    }

    private def initFrom(other: Ohlc) {
        O = other.O
        H = other.H
        L = other.L
        C = other.C
        Volume = other.Volume
        Oi = other.Oi
    }

    def initFrom(other: Tick) {
        O = other.last
        H = other.last
        L = other.last
        C = other.last
        Volume = other.vol
        Oi = 0
    }

    def interpolated: Boolean = Volume == 0

    private def addPrice(last: Double) {
        if (O.isNaN) O = last
        if (H < last) {
            H = last
        }
        if (L > last) {
            L = last
        }
        C = last
    }

    def addTick(tick: Tick) {
        if (interpolated) {
            initFrom(tick)
        }
        //FIXME
        if(tick.last.isNaN){
            tick.last = (tick.bid + tick.ask)/2;
        }
        addPrice(tick.last)
        Volume += tick.vol
    }

    def addOhlc(ohlc: Ohlc) {
        if (ohlc.interpolated) {
            return
        }
        if (interpolated) {
            initFrom(ohlc)
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

    def isUpBar: Boolean = C > O

    def range: Double = H - L

    def upShadow: Double = H - C

    def downShadow: Double = C - L

    def bodyLength: Double = Math.abs(C - O)

    def Return: Double = C - O

    def isInRange(vv: Double): Boolean = H > vv && vv > L

    override def toString: String = {
        val dtStr: String = dtGmtEnd.toStandardString
        return s"OHLC($O/$H/$L/$C/$dtStr/$interpolated)"
    }

    def interpolateFrom(prev: Ohlc) {
        initFrom(prev)
        Volume = 0
    }

    def DtGmt: Instant = dtGmtEnd

    @BeanProperty
    var C: Double = .0
    @BeanProperty
    var dtGmtEnd: Instant = null
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
