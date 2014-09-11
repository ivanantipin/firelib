package firelib.common.factors

import firelib.common.misc.utils

/**

 */
class SimpleFactor(val name: String, func: () => Double) extends Factor {

    def value: String = {
        try {
            return utils.dbl2Str(func(), 10)
        }
        catch {
            case e: Exception => return Double.NaN.toString
        }
    }
}
