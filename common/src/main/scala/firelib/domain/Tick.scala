package firelib.domain

import java.time.Instant

import firelib.common.Side

import scala.beans.BeanProperty

class Tick() extends Comparable[Tick] with Timed {

    private def this(last: Double, vol: Int, ssi: Int, bid: Double, ask: Double, tickNumber: Int, dtGmt: Instant) {
        this()
        this.last = last
        this.vol = vol
        this.securityStreamId = ssi
        this.bid = bid
        this.ask = ask
        this.dtGmt = dtGmt
        this.tickNumber = tickNumber
    }

    override def toString: String = {
        return s"Tick(B:$bid A:$ask L:$last V:$vol T$dtGmt"
    }

    def compareTo(o: Tick): Int = {
        return dtGmt.compareTo(o.dtGmt)
    }

    def DtGmt: Instant = {
        return dtGmt
    }

    @BeanProperty
    var ask: Double = Double.NaN
    @BeanProperty
    var bid: Double = Double.NaN
    @BeanProperty
    var last: Double = Double.NaN
    @BeanProperty
    var securityStreamId: Int = 0
    @BeanProperty
    var vol: Int = 0
    @BeanProperty
    var side: Side = null
    @BeanProperty
    var tickNumber: Int = 0
    @BeanProperty
    var dtGmt: Instant = null
}
