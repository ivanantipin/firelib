package firelib.common

import org.joda.time.DateTime

case class Tick(val last: Double, var Vol: Int = 0, val SecurityStreamId: Int = 1, val DtGmt: DateTime,
                var bid: Double = Double.NaN, var ask: Double = Double.NaN, var tickNumber: Int = -1, var TickSide: Side = Side.None) extends Timed{
}
