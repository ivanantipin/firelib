package firelib.common.core

import firelib.common.config.ModelConfig
import firelib.common.interval.IntervalServiceComponent
import firelib.common.marketstub.MarketStubFactoryComponent
import firelib.common.mddistributor.MarketDataDistributorComponent
import firelib.common.reader.ReadersFactoryComponent
import firelib.common.timeboundscalc.TimeBoundsCalculatorComponent


class SimpleRunCtx(val modelConfig: ModelConfig) extends  OnContextInited
with BacktestComponent
with TimeBoundsCalculatorComponent
with ModelConfigContext
with ReadersFactoryComponent
with StepServiceComponent
with MarketDataDistributorComponent
with IntervalServiceComponent
with BindModelComponent
with MarketStubFactoryComponent {

}