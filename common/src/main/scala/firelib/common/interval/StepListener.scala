package firelib.common.interval

import java.time.Instant

/**
 * Created by ivan on 9/5/14.
 */
trait StepListener {
    def onStep(dtGmt:Instant)
}
