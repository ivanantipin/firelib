package firelib.backtest

import firelib.common._

object BacktestStarter {
    def Start(mc: ModelConfig) {
        mc.Mode match {
            case ResearchMode.InOutSample => new BacktesterOptimized().Run(mc)
            case ResearchMode.SimpleRun => new BacktesterSimple().Run(mc);
            case ResearchMode.FwdTesting => throw new RuntimeException("fwd testing not supported yet")
        }
    }
}
