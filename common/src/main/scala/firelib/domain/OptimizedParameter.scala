package firelib.common

class OptimizedParameter(val Name: String, val Start: Int, val End: Int, val Step: Int = 1) {

    def getVariations(): List[Int] = List.range(Start, End, Step)
}
