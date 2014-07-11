package firelib.domain

object MarketDataType extends Enumeration {


  trait MarketDataTypeEx {
    def IsTick: Boolean = {
      if (this == Tick)
        return true;
      return false;
    }

    def IsDepth: Boolean = {
      if (this == MarketDepth)
        return true;
      return false;
    }

    def IsOhlc: Boolean = {
      return !(IsTick || IsDepth);
    }

    def ToMin: Int = {
      return if (IsTick || this == Ohlc1S) 0 else Value.id
    }

    /*public static Interval ToInterval(this MarketDataType type)
    {
      return Interval.ResolveFromMs(type.ToSec()*1000);
    }*/


    def ToSec(): Int = {
      return Value.id
    }
  }

  type MarketDataType = Value with MarketDataTypeEx

  val MarketDepth = MarketDataType(-2)
  val None = MarketDataType(-1)
  val Tick = MarketDataType(0)
  val Ohlc1S = MarketDataType(1)
  /*val Ohlc10S = Value(10)
  val Ohlc30S = Value(30)
  val Ohlc1M = Value(60)
  val Ohlc2M = Value(60 * 2)
  val Ohlc3M = Value(60 * 3)
  val Ohlc4M = Value(60 * 4)
  val Ohlc5M = Value(60 * 5)
  val Ohlc10M = Value(60 * 10)
  val Ohlc15M = Value(60 * 15)
  val Ohlc30M = Value(60 * 30)
  val Ohlc60M = Value(60 * 60)
  val Ohlc120M = Value(60 * 120)
  val Ohlc240M = Value(60 * 240)
  val Day = Value(60 * 1440)*/


}

