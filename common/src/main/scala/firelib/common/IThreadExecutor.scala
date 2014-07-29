package firelib.common

trait IThreadExecutor {

    def Execute(task: => Unit)

    def Start(): IThreadExecutor

    def Stop()
}
