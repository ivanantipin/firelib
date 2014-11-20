package firelib.common.interval

import java.time.Instant

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


class IntervalServiceImpl extends IntervalService {

    val nodes = mutable.TreeSet.empty(Ordering.by[Node,Long](a=>a.interval.durationMs).reverse)

    var rootNode : Node =_

    class Node(val interval : Interval){
        val childs = new ArrayBuffer[Node](3)
        val listeners = new ArrayBuffer[Instant => Unit]()

        def onStep(ms : Long, dt : Instant) : Unit = {
            if (ms  % interval.durationMs == 0) {
                listeners.foreach(_(dt))
                if(childs.nonEmpty)
                    childs.foreach(_.onStep(ms,dt))
            }
        }
    }
    def addListener(interval: Interval, action: Instant  => Unit) = {
        addOrGetIntervalNode(interval).listeners += action
        if(nodes.last != rootNode){
            rootNode = nodes.last
        }
    }

    def addOrGetIntervalNode(interval: Interval): Node = {
        nodes.find(n => n.interval == interval) match {
            case Some(node) => node
            case None => {
                val node = new Node(interval)
                nodes.find(par => depends(par.interval, interval)) match {
                    case Some(parNode) => parNode.childs += node
                    case None => nodes += node
                }
                node
            }
        }

    }

    def depends(parent : Interval, child : Interval) : Boolean = {
        child.durationMs % parent.durationMs == 0
    }

    def removeListener(interval: Interval, action: Instant  => Unit): Unit = {
        addOrGetIntervalNode(interval).listeners -= action
    }

    def onStep(dt:Instant) = {
        rootNode.onStep(dt.toEpochMilli,dt)
    }
}
