package firelib.common

import java.time.Instant

import scala.collection.mutable.ArrayBuffer


class IntervalService extends IIntervalService {

    private val actions = new ArrayBuffer[(Interval, ArrayBuffer[Instant => Unit])]()

    def addListener(interval: Interval, action: Instant  => Unit) = {
        var tuple = actions.find(_._1 == interval)
        if (tuple.isEmpty) {
            actions += ((interval, new ArrayBuffer[Instant => Unit]()))
            tuple = actions.find(_._1 == interval)
        }
        tuple.get._2 += action
    }

    def removeListener(interval: Interval, action: Instant  => Unit): Unit = {
        actions.find(_._1 == interval).get._2 -= action
    }

    def onStep(dt:Instant) = {
        actions.foreach(action => {
            if (dt.toEpochMilli  % action._1.durationMs == 0) {
                action._2.foreach(_(dt))
            }
        })
    }
}
