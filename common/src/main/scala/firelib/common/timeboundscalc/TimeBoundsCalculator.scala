package firelib.common.timeboundscalc

import java.time.Instant

import firelib.common.config.ModelConfig

/**

 */
trait TimeBoundsCalculator extends (ModelConfig=>(Instant,Instant))
