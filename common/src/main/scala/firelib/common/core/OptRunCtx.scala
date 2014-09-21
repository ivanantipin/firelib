package firelib.common.core

import firelib.common.config.ModelConfig
import firelib.common.marketstub.MarketStubFactoryComponent
import firelib.common.misc.TickToPriceConverterComponent
import firelib.common.reader.ReadersFactoryComponent
import firelib.common.timeboundscalc.TimeBoundsCalculatorComponent


/**

 */
class OptRunCtx(val config : ModelConfig) extends OptimizerComponent
    with ModelConfigContext
    with  EnvFactoryComponent
    with MarketStubFactoryComponent
    with TimeBoundsCalculatorComponent
    with TickToPriceConverterComponent
    with ReadersFactoryComponent{
    override val modelConfig = config
}
