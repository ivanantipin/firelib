package firelib.backtest

import firelib.common._

object backtestStarter {
    def Start(mc: ModelConfig) {
        val readerFactory: DefaultReaderFactory = new DefaultReaderFactory(mc.dataServerRoot)
        val backtestEnvFactory: DefaultBacktestEnvFactory = new DefaultBacktestEnvFactory(readerFactory, defaultTimeBoundsCalculator)
        mc.mode match {
            case ResearchMode.InOutSample => new BacktesterOptimized(backtestEnvFactory, defaultMarketStubFactory).run(mc)
            case ResearchMode.SimpleRun => new BacktesterSimple(backtestEnvFactory, defaultMarketStubFactory).run(mc)
            case ResearchMode.FwdTesting => throw new RuntimeException("fwd testing not supported yet")
            case _=>throw new RuntimeException("not possible")
        }
    }
}
