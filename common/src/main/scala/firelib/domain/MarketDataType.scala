package firelib.common

sealed class MarketDataType private (val Name: String) {
    override def toString: String = Name
}

object MarketDataType {
    val Tick = new MarketDataType("Tick")
    val Ohlc = new MarketDataType("Ohlc")
    val Depth = new MarketDataType("Depth")
}

