package firelib.common

trait IThreadExecutor {

    def execute(task: => Unit)

    def start(): IThreadExecutor

    def stop()
}
