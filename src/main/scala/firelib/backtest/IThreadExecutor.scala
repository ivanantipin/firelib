package firelib.backtest

trait IThreadExecutor {

    def Execute(task: => Unit);

    def Start();

    def Stop();
}
