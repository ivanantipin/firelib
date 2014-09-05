package firelib.common.core

import firelib.common.config.ModelConfig
import firelib.common.core.{BacktestMode, OptRunCtx, SimpleRunCtx}

/**
 * Created by ivan on 9/5/14.
 */
object backtestStarter {
    def runBacktest(mc: ModelConfig) {
        try{
            mc.backtestMode match {
                case BacktestMode.InOutSample => new OptRunCtx(mc.dataServerRoot).optimizer.run(mc)
                case BacktestMode.SimpleRun => new SimpleRunCtx(mc.dataServerRoot).backtesterSimple.run(mc)
                case BacktestMode.FwdTesting => throw new RuntimeException("fwd testing not supported yet")
                case _=>throw new RuntimeException("not possible")
            }
            System.out.println()

        }catch {
            case ex : Throwable => ex.printStackTrace()
        }
    }
}
