package firelib.domain

trait TickerType extends Enumeration {
  type TickerType = Value
  val Ohlc, Tick = Value
}
