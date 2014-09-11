package firelib.common.interval

/**

 */
trait IntervalServiceComponent {
    val intervalService : IntervalService = new IntervalServiceImpl
}
