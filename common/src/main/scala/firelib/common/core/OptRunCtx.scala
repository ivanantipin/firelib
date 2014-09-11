package firelib.common.core

import firelib.common.marketstub.MarketStubFactoryComponent
import firelib.common.reader.ReadersFactoryComponent
import firelib.common.timeboundscalc.TimeBoundsCalculatorComponent


/**

 */
class OptRunCtx(val dsRoot: String) extends OptimizerComponent
    with  EnvFactoryComponent
    with MarketStubFactoryComponent
    with TimeBoundsCalculatorComponent
    with ReadersFactoryComponent{}
