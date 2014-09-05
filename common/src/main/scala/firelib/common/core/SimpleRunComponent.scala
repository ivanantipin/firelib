package firelib.common.core

import firelib.common.config.ModelConfig
import firelib.common.marketstub.{MarketStub, MarketStubFactoryComponent}
import firelib.common.report.reportWriter

import scala.collection.mutable.ArrayBuffer

/**
 * simple backtest flow without optimizations backtest and dump report
 */

trait SimpleRunComponent{

    this : EnvFactoryComponent with MarketStubFactoryComponent =>

    val backtesterSimple  = new BacktesterSimple()

    class BacktesterSimple  {

        def run(cfg: ModelConfig) : BacktestEnvironment = {

            val env : BacktestEnvironment = envFactory.apply(cfg)

            val model = cfg.newModelInstance()

            val stubs: ArrayBuffer[MarketStub] = cfg.tickerConfigs.map(marketStubFactory)

            env.bindModel(model,stubs,cfg.customParams.toMap)

            env.backtest()

            reportWriter.write(model, cfg, cfg.reportTargetPath)

            return env
        }

    }
}
