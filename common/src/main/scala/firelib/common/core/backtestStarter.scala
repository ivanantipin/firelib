package firelib.common.core

import firelib.common.config.ModelConfig

/**

 */
object backtestStarter {
    def runBacktest(mc: ModelConfig) {
        try{
            mc.backtestMode match {
                case BacktestMode.InOutSample => new BacktesterOptimized().run(mc)
                case BacktestMode.SimpleRun => new BacktesterSimple().run(mc)
                case BacktestMode.FwdTesting => throw new RuntimeException("fwd testing not supported yet")
                case _=>throw new RuntimeException("not possible")
            }
            System.out.println()

        }catch {
            case ex : Throwable => ex.printStackTrace()
        }
    }
}
