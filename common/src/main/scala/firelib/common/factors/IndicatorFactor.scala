package firelib.common.factors

import firelib.common.misc.utils
import firelib.indicators.Indicator

/**
 * Created by ivan on 8/15/14.
 */
class IndicatorFactor(indicator: Indicator[Double], val name: String, decimalPlaces: Int = 10) extends Factor {

    def value: String = {
        try {
            return utils.dbl2Str(indicator.value, decimalPlaces)
        }
        catch {
            case e: Exception => return Double.NaN.toString
        }
    }


}
