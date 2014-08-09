package firelib.utils


class Node(val Value: Double, var Left: Boolean){}

class HeapQuantile(val quantile: Double, val length: Int) {
/*

    val ring = new mutable.Queue[Node]()(length)

    private val ordering: Ordering[Node] = Ordering.fromLessThan[Node](_.Value < _.Value)

    private val left = new mutable.TreeSet[Node]()(ordering)
    private val right = new mutable.TreeSet[Node]()(ordering)

    private var cnt = 0


    def Count = cnt

    def GetValue(): Double = {


        if (left.isEmpty && right.isEmpty) {
            return 0
       }
        if (left.isEmpty) {
            return right.head.Value
       }
        if (right.isEmpty) {
            return left.last.Value
       }
        return (left.last.Value + right.head.Value) / 2
   }

    def addMetric(m: Double) = {
        if (ring.length >= length) {
            val node = ring.dequeue()
            if (node.Left) {
                left.remove(node)
           }
            else {
                left.remove(node)
           }
        }
        cnt += 1

        val valu = GetValue()

       if (m > valu) {
            var n = new Node(m, Left = false)
           ring += n
            right += n
        }
        else {
            var n = new Node(m, Left = true)
           ring += n
            left += n
        }
        balance()
    }

   private def balance() {
        val diff = left.size / quantile - right.size / (1 - quantile)
        if(diff > 0) {
            val amDiff = (left.size - 1) / quantile - (right.size + 1) / (1 - quantile)
           if (math.abs(amDiff) < math.abs(diff)) {
                var leftMax = left.lastKey
                left.remove(leftMax)
                right += leftMax
                leftMax.Left = false

        }
        else {
            val amDiff = (left.size + 1) / quantile - (right.size - 1) / (1 - quantile)
            i (math.abs(amDiff) <  math.abs(diff)) {
                var rightMin = right.firstKey
                right.remove(rightMin)
                left += rightMin
               rightMin.Left = true
            }
        }
    }
*/
}
