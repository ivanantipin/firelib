package firelib.common

import java.time.Instant

trait IIntervalService extends IStepListener {
    def AddListener(interval: Interval, action: Instant  => Unit)

}
