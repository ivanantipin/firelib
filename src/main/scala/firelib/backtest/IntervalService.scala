package firelib.backtest

import firelib.domain.Interval
import org.joda.time.DateTime

import scala.collection.mutable.ArrayBuffer


class IntervalService extends IIntervalService {
    val actions = new ArrayBuffer[(Interval, ArrayBuffer[DateTime => Unit])]();

    def AddListener(interval: Interval, action: DateTime => Unit) = {
        var tuple = actions.find(_._1 == interval)
        if (tuple.isEmpty) {
            actions += ((interval, new ArrayBuffer[DateTime => Unit]()))
            tuple = actions.find(_._1 == interval)
        }
        tuple.get._2 += action
    }

    def RemoveListener(interval: Interval, action: DateTime => Unit): Unit = {
        actions.find(_._1 == interval).get._2 -= action;
    }

    def OnStep(dt: DateTime) = {
        actions.foreach(action => {
            if (dt.getMillis % action._1.durationMs == 0) {
                action._2.foreach(_(dt))
            }
        })
    }
}
