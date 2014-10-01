package firelib.common.interval

import firelib.common.core.StepServiceComponent

/**

 */
trait IntervalServiceComponent {
    this : StepServiceComponent =>
    val intervalService : IntervalService = new IntervalServiceImpl
    stepService.listen(intervalService)
}
