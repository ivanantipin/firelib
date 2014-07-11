import firelib.domain.{MarketDataType, Direction}
import firelib.domain.Direction.Direction

object HelloWorld {
  /* This is my first java program.
   * This will print 'Hello World' as the output
   */

  def sss(dir: Direction) {
    if (dir == Direction.Down) {
      System.out.println("sss")
    }
  }

  def main(args: Array[String]) {
    val mdt = MarketDataType.Ohlc1S
    System.out.println(mdt.)

  }
}