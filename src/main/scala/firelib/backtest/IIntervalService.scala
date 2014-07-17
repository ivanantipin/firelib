package firelib.backtest

import firelib.domain.Interval
import org.joda.time.DateTime

trait IIntervalService extends IStepListener {
    def AddListener(interval: Interval, action: DateTime => Unit)

}
