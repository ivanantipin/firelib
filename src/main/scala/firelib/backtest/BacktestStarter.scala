package firelib.backtest

import firelib.domain.ResearchMode

object BacktestStarter {
    def Start(mc: ModelConfig) {
        mc.Mode match {
            case ResearchMode.InOutSample => new BacktesterOptimized().Run(mc)
            case ResearchMode.SimpleRun => new BacktesterSimple().Run(mc);
        }
    }
}
