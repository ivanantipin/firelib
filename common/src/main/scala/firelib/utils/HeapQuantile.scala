package firelib.utils

import scala.collection.mutable


class HeapQuantile(val quantile: Double, val length: Int) {

    class Node(val value: Double, var isLeft: Boolean) {}

    private val ring = new mutable.Queue[Node]()

    private val ordering: Ordering[Node] = Ordering.fromLessThan[Node](_.value < _.value)

    private val left = new mutable.TreeSet[Node]()(ordering)
    private val right = new mutable.TreeSet[Node]()(ordering)

    private var cnt = 0

    def count = cnt

    def value: Double = {
        if (left.isEmpty && right.isEmpty) {
            return 0
        }
        if (left.isEmpty) {
            return right.firstKey.value
        }
        if (right.isEmpty) {
            return left.lastKey.value
        }
        return (left.lastKey.value + right.firstKey.value) / 2
    }

    def addMetric(m: Double) = {
        if (ring.size >= length) {
            val node = ring.dequeue()
            if (node.isLeft) {
                left.remove(node)
            }
            else {
                right.remove(node)
            }
        }
        cnt += 1

        if (m > value) {
            var n = new Node(m, false)
            ring += n
            right += n
        }
        else {
            var n = new Node(m, true)
            ring += n
            left += n
        }
        balance()

        if (false) {
            if (left.size > 2) {
                assert(left.firstKey.value <= left.lastKey.value)
            }
            if (right.size > 2) {
                assert(right.firstKey.value <= right.lastKey.value)
            }
            if (left.size > 2 && right.size > 2) {
                assert(left.lastKey.value <= right.firstKey.value)
            }
        }
    }

    private def balance() {
        val diff = left.size / quantile - right.size / (1 - quantile)
        if (diff > 0) {
            val amDiff = (left.size - 1) / quantile - (right.size + 1) / (1 - quantile)
            if (math.abs(amDiff) < math.abs(diff)) {
                var leftMax = left.lastKey
                left.remove(leftMax)
                right += leftMax
                leftMax.isLeft = false

            }
        }
        else {
            val amDiff = (left.size + 1) / quantile - (right.size - 1) / (1 - quantile)
            if (math.abs(amDiff) < math.abs(diff)) {
                var rightMin = right.firstKey
                right.remove(rightMin)
                left += rightMin
                rightMin.isLeft = true
            }
        }
    }
}
