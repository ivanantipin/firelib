package firelib.common

import java.time.Instant

trait IIntervalService extends IStepListener {
    def addListener(interval: Interval, action: Instant  => Unit)

}
