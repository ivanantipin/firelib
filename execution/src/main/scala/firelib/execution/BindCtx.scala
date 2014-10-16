package firelib.execution

import firelib.common.config.ModelBacktestConfig
import firelib.common.core.{BindModelComponent, ModelConfigContext, OnContextInited, StepServiceComponent}
import firelib.common.interval.IntervalServiceComponent
import firelib.common.mddistributor.MarketDataDistributorComponent


class BindCtx(val modelConfig : ModelBacktestConfig) extends OnContextInited
with BindModelComponent
with ModelConfigContext
with MarketDataDistributorComponent
with StepServiceComponent
with IntervalServiceComponent{

}
