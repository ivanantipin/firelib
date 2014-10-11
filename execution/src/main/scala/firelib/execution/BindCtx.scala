package firelib.execution

import firelib.common.config.ModelConfig
import firelib.common.core.{BindModelComponent, ModelConfigContext, OnContextInited, StepServiceComponent}
import firelib.common.interval.IntervalServiceComponent
import firelib.common.mddistributor.MarketDataDistributorComponent


class BindCtx(val modelConfig : ModelConfig) extends OnContextInited
with BindModelComponent
with ModelConfigContext
with MarketDataDistributorComponent
with StepServiceComponent
with IntervalServiceComponent{

}
