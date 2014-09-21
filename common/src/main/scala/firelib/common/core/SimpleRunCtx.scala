package firelib.common.core

import firelib.common.config.ModelConfig
import firelib.common.marketstub.MarketStubFactoryComponent
import firelib.common.misc.TickToPriceConverterComponent
import firelib.common.reader.ReadersFactoryComponent
import firelib.common.timeboundscalc.TimeBoundsCalculatorComponent

/**
 * runs backtest for provided model config
 * uses default behaviour
 * to customize reader factory how time bounds calculated need to reimplement factories
 */

class SimpleRunCtx(val config : ModelConfig) extends SimpleRunComponent
        with ModelConfigContext
        with  EnvFactoryComponent
        with MarketStubFactoryComponent
        with TimeBoundsCalculatorComponent
        with TickToPriceConverterComponent
        with ReadersFactoryComponent{
        override val modelConfig = config
}
