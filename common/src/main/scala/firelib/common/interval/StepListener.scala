package firelib.common.interval

import java.time.Instant

/**

 */
trait StepListener {
    def onStep(dtGmt:Instant)
}
