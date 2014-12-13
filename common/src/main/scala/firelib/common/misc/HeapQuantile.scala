package firelib.common.misc

import java.util
import java.util.Comparator

import scala.collection.mutable




class  HeapQuantile(val quantile: Double, val length: Int) {

    case class Node(val value: Double, var isLeft: Boolean)

    class NodeComparator extends Comparator[Node]{
        override def compare(o1: Node, o2: Node): Int = o1.value.compareTo(o2.value)
    }

    private val ring = new mutable.Queue[Node]()

    private val left = new util.TreeSet[Node](new NodeComparator)
    private val right = new util.TreeSet[Node](new NodeComparator)

    private var cnt = 0

    def count = cnt

    def value: Double = {
        if (left.isEmpty && right.isEmpty) {
            return 0
        }
        if (left.isEmpty) {
            return right.first().value
        }
        if (right.isEmpty) {
            return left.last().value
        }
        return (left.last().value + right.first().value) / 2
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
            right.add(n)
        }
        else {
            var n = new Node(m, true)
            ring += n
            left.add(n)
        }
        balance()
    }

    private def balance() {
        val diff = left.size / quantile - right.size / (1 - quantile)
        if (diff > 0) {
            val amDiff = (left.size - 1) / quantile - (right.size + 1) / (1 - quantile)
            if (math.abs(amDiff) < math.abs(diff)) {
                var leftMax = left.last()
                left.remove(leftMax)
                right.add(leftMax)
                leftMax.isLeft = false

            }
        }
        else {
            val amDiff = (left.size + 1) / quantile - (right.size - 1) / (1 - quantile)
            if (math.abs(amDiff) < math.abs(diff)) {
                var rightMin = right.first()
                right.remove(rightMin)
                left.add(rightMin)
                rightMin.isLeft = true
            }
        }
    }
}
