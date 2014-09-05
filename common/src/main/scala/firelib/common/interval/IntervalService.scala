package firelib.common.interval

import java.time.Instant

import firelib.common.interval.Interval

/**
 * Created by ivan on 9/5/14.
 */
trait IntervalService extends StepListener {
    def addListener(interval: Interval, action: Instant => Unit)
}
