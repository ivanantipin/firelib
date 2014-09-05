package firelib.common.interval

/**
 * Created by ivan on 9/5/14.
 */
trait IntervalServiceComponent {
    val intervalService : IntervalService = new IntervalServiceImpl
}
