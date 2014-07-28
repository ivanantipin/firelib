package firelib.common

import java.time.Instant

import scala.beans.BeanProperty

class Tick() extends Comparable[Tick] with Timed {

    private def this(last: Double, vol: Int, ssi: Int, bid: Double, ask: Double, tickNumber: Int, dtGmt: Instant) {
        this()
        this.Last = last
        this.Vol = vol
        this.SecurityStreamId = ssi
        this.Bid = bid
        this.Ask = ask
        this.dtGmt = dtGmt
        TickNumber = tickNumber
    }

    override def toString: String = {
        return "Tick(B:%s:A:%s:L%sV%sT%s".format(Bid, Ask, Last, Vol, dtGmt.toString)
    }

    def compareTo(o: Tick): Int = {
        return dtGmt.compareTo(o.dtGmt)
    }

    def DtGmt: Instant = {
        return dtGmt
    }

    @BeanProperty
    var Ask: Double = Double.NaN
    @BeanProperty
    var Bid: Double = Double.NaN
    @BeanProperty
    var Last: Double = Double.NaN
    @BeanProperty
    var SecurityStreamId: Int = 0
    @BeanProperty
    var Vol: Int = 0
    @BeanProperty
    var side: Side = null
    @BeanProperty
    var TickNumber: Int = 0
    @BeanProperty
    var dtGmt: Instant = null
}
