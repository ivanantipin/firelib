package firelib.domain

trait IFactor {
    def Name: String;

    def Value: String;
}

class SimpleFactor(val Name: String, func: () => Double) extends IFactor {

    def Value: String = {
        try {
            return Utils.Dbl2Str(func(), 10);
        }
        catch {
            case e: Exception => return "" + Double.NaN;
        }
    }
}


class IndicatorFactor(indicator: IIndicator[Double], val Name: String, decimalPlaces: Int = 10) extends IFactor {

    def Value: String = {
        try {
            return Utils.Dbl2Str(indicator.Value, decimalPlaces);
        }
        catch {
            case e: Exception => return "" + Double.NaN;
        }
    }


}
