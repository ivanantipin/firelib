package firelib.common.core

import firelib.common.config.ModelBacktestConfig
import firelib.common.report.reportWriter
/**
 * simple backtest flow without optimizations backtest and dump report
 */

class BacktesterSimple  {

    def run(cfg: ModelBacktestConfig) : Unit = {

        val ctx: SimpleRunCtx = new SimpleRunCtx(cfg)

        ctx.init()

        ctx.bindModelForParams(cfg.modelParams.toMap)


        ctx.backtest.backtest()

        reportWriter.write(ctx.models(0), cfg, cfg.reportTargetPath)

    }

}
