package firelib.backtest

import firelib.common._

object BacktestStarter {
    def Start(mc: ModelConfig) {
        mc.mode match {
            case ResearchMode.InOutSample => new BacktesterOptimized().run(mc)
            case ResearchMode.SimpleRun => new BacktesterSimple().run(mc)
            case ResearchMode.FwdTesting => throw new RuntimeException("fwd testing not supported yet")
        }
    }
}
