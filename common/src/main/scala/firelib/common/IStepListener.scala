package firelib.common

import java.time.Instant

trait IStepListener {
    def OnStep(dtGmt:Instant)
}
