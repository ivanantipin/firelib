package firelib.domain

object OrderType extends Enumeration {
  type OrderType = Value
  val Limit, Market, Stop = Value
}
