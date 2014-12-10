package firelib.execution

import firelib.common.threading.ThreadExecutor

trait Configurable{

    /**
     * pass configuration params to gate
    * usually it is user/password, broker port and url etc
    */
    def start(config: Map[String, String], callbackExecutor: ThreadExecutor)
}
