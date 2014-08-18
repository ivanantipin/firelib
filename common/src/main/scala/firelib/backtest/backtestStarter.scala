package firelib.backtest

import firelib.common._

/**
 * runs backtest for provided model config
 * uses default behaviour
 * to customize reader factory how time bounds calculated need to reimplement factories
 */
object backtestStarter {
    def runBacktest(mc: ModelConfig) {
        try{
            val readerFactory: DefaultReaderFactory = new DefaultReaderFactory(mc.dataServerRoot)
            val backtestEnvFactory: DefaultBacktestEnvFactory = new DefaultBacktestEnvFactory(readerFactory, defaultTimeBoundsCalculator)
            mc.backtestMode match {
                case BacktestMode.InOutSample => new BacktesterOptimized(backtestEnvFactory, defaultMarketStubFactory).run(mc)
                case BacktestMode.SimpleRun => new BacktesterSimple(backtestEnvFactory, defaultMarketStubFactory).run(mc)
                case BacktestMode.FwdTesting => throw new RuntimeException("fwd testing not supported yet")
                case _=>throw new RuntimeException("not possible")
            }
            System.out.println()

        }catch {
            case ex : Throwable => ex.printStackTrace()
        }
    }
}
