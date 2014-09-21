package firelib.domain

import java.time.Instant

import firelib.common.misc.dateUtils
import firelib.common.misc.dateUtils._

import scala.beans.BeanProperty

class Ohlc() extends Timed {

    var interpolated: Boolean = true

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


    def DtGmt: Instant = dtGmtEnd

    @BeanProperty
    var C: Double = .0
    @BeanProperty
    var dtGmtEnd: Instant = Instant.MIN
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
