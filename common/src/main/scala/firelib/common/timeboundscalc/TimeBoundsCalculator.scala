package firelib.common.timeboundscalc

import java.time.Instant

import firelib.common.config.ModelConfig

/**
 * Created by ivan on 9/5/14.
 */
trait TimeBoundsCalculator extends (ModelConfig=>(Instant,Instant))
