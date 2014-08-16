package firelib.common

/**
 * optimized parameter configuration - support only int values
 * @param name - name of parameter
 * @param start - start of range
 *@param end - end of range
 * @param step - step
 */
class OptimizedParameter(val name: String, val start: Int, val end: Int, val step: Int = 1) {

    def getVariations(): List[Int] = List.range(start, end, step)
}
