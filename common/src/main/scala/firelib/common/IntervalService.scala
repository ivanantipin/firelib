package firelib.common

import java.time.Instant

import scala.collection.mutable.ArrayBuffer


class IntervalService extends IIntervalService {

    private val listeners = new ArrayBuffer[(Interval, ArrayBuffer[Instant => Unit])]()

    def addListener(interval: Interval, action: Instant  => Unit) = {
        var tuple = listeners.find(_._1 == interval)
        if (tuple.isEmpty) {
            listeners += ((interval, new ArrayBuffer[Instant => Unit]()))
            tuple = listeners.find(_._1 == interval)
        }
        tuple.get._2 += action
    }

    def removeListener(interval: Interval, action: Instant  => Unit): Unit = {
        listeners.find(_._1 == interval).get._2 -= action
    }

    def onStep(dt:Instant) = {
        listeners.foreach(action => {
            if (dt.toEpochMilli  % action._1.durationMs == 0) {
                action._2.foreach(_(dt))
            }
        })
    }
}
