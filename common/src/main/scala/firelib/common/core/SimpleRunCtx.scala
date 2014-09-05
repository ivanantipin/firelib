package firelib.common.core

import firelib.common.marketstub.MarketStubFactoryComponent
import firelib.common.reader.ReadersFactoryComponent
import firelib.common.timeboundscalc.{TimeBoundsCalculatorComponent}

/**
 * runs backtest for provided model config
 * uses default behaviour
 * to customize reader factory how time bounds calculated need to reimplement factories
 */

class SimpleRunCtx(val dsRoot: String) extends SimpleRunComponent
        with  EnvFactoryComponent
        with MarketStubFactoryComponent
        with TimeBoundsCalculatorComponent
        with ReadersFactoryComponent{}
