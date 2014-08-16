package firelib.domain

import firelib.common.{IFactor, utils}

/**
 * Created by ivan on 8/15/14.
 */
class IndicatorFactor(indicator: IIndicator[Double], val name: String, decimalPlaces: Int = 10) extends IFactor {

    def value: String = {
        try {
            return utils.dbl2Str(indicator.value, decimalPlaces)
        }
        catch {
            case e: Exception => return Double.NaN.toString
        }
    }


}
