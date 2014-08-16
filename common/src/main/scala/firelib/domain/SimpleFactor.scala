package firelib.domain

import firelib.common.{IFactor, utils}

/**
 * Created by ivan on 8/15/14.
 */
class SimpleFactor(val name: String, func: () => Double) extends IFactor {

    def value: String = {
        try {
            return utils.dbl2Str(func(), 10)
        }
        catch {
            case e: Exception => return Double.NaN.toString
        }
    }
}
