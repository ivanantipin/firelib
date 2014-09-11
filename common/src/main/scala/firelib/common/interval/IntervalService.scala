package firelib.common.interval

import java.time.Instant

/**
 * interval service to check intervals end and send notification to listeners
 */
trait IntervalService extends StepListener {
    def addListener(interval: Interval, action: Instant => Unit)
}
