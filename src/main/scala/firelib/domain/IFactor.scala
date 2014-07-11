package firelib.domain

import com.firelib.util.Utils
import firelib.indicator.IIndicator

trait IFactor {
  def Name: String;

  def Value: String;
}

class SimpleFactor(name: String, func: () => Double) extends IFactor {

  def Name: String = {
    return name
  }

  def Value: String = {
    try {
      return Utils.Dbl2Str(func(), 10);
    }
    catch {
      case e: Exception => return "" + Double.NaN;
    }
  }
}


class IndicatorFactor(indicator: IIndicator[Double], name: String, decimalPlaces: Int = 10) extends IFactor {

  def Name: String = {
    return name
  }

  def Value: String = {
    try {
      return Utils.Dbl2Str(indicator.Value, decimalPlaces);
    }
    catch {
      case e: Exception => return "" + Double.NaN;
    }
  }


}
