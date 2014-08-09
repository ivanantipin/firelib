package firelib.common



import firelib.indicator.IIndicator

trait IFactor {
    def name: String

    def value: String
}

class SimpleFactor(val name: String, func: () => Double) extends IFactor {

    def value: String = {
        try {
            return Utils.dbl2Str(func(), 10)
        }
        catch {
            case e: Exception => return "" + Double.NaN
        }
    }
}


class IndicatorFactor(indicator: IIndicator[Double], val name: String, decimalPlaces: Int = 10) extends IFactor {

    def value: String = {
        try {
            return Utils.dbl2Str(indicator.Value, decimalPlaces)
        }
        catch {
            case e: Exception => return "" + Double.NaN
        }
    }


}
