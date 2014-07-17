package firelib.backtest

trait IThreadExecutor {

    def Execute(task: Unit => Unit);

    def Start();

    def Stop();
}
