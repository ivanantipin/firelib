package firelib.backtest

import org.joda.time.DateTime

trait IStepListener {
    def OnStep(dtGmt: DateTime)
}
