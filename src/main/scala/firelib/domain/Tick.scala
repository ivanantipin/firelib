package firelib.domain

import org.joda.time.DateTime

class Tick(val last: Double, val Vol: Int, val SecurityStreamId: Int, val DtGmt: DateTime, val bid: Double, val ask: Double, val tickNumber: Int, val dtGmt: DateTime, val TickSide: Side) {
    def ToString: String = {
        return ""
        //return string.Format("Tick(B:{0}:A:{1}:L{2}V{3}T{4})", Bid, Ask, Last, Vol,
        //                   Dt.ToString("dd.MM.yyyy HH:mm:ss "));
    }
}
