package firelib.domain

import org.joda.time.DateTime

case class Tick(val last: Double, val Vol: Int, val SecurityStreamId: Int, val DtGmt: DateTime,
                val bid: Double, val ask: Double, val tickNumber: Int, val dtGmt: DateTime, val TickSide: Side) extends Timed{
}
