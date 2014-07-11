
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class OptimizedParameter(val Name: String, val Start: Int, val End: Int, val Step: Int = 1) {

  def GetVariations: ArrayBuffer[Int] = {
    var ret = new mutable.ArrayBuffer[Int]();

    for (a <- 1 until End by Step) {
      ret += a
    }
    return ret;
  }
}
