package firelib.backtest

import firelib.domain.ResearchModeEnum

object BacktestStarter {
    def Start(mc: ModelConfig) {
        mc match {
            case ResearchModeEnum.InOutSample => new BacktesterOptimized().Run(mc)
            case ResearchModeEnum.SimpleRun => new BacktesterSimple().Run(mc);
        }
    }
}
