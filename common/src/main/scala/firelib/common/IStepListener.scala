package firelib.common

import java.time.Instant

trait IStepListener {
    def onStep(dtGmt:Instant)
}
