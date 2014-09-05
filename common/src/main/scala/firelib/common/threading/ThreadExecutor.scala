package firelib.common.threading

/**
 * Created by ivan on 9/5/14.
 */
trait ThreadExecutor {

    def execute(task: () => Unit)

    def start(): ThreadExecutor

    def shutdown()
}
