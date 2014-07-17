package firelib.domain

object OrderTypeEnum {
    val Limit = new OrderType("Limit")
    val Market = new OrderType("Market")
    val Stop = new OrderType("Stop")
}

sealed class OrderType(val Name: String) {
    override def toString: String = Name
}



