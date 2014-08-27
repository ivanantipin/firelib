package firelib.backtest

import firelib.common._
import firelib.report.reportWriter

import scala.collection.mutable.ArrayBuffer


/**
 * simple backtest flow without optimizations backtest and dump report
 * @param envFactory
 * @param stubFactory
 */

class BacktesterSimple (envFactory : BacktestEnvironmentFactory, stubFactory : MarketStubFactory) {

    def run(cfg: ModelConfig) : BacktestEnvironment = {

        val env : BacktestEnvironment = envFactory(cfg)

        val model = cfg.newModelInstance()

        val stubs: ArrayBuffer[IMarketStub] = cfg.tickerConfigs.map(stubFactory)

        env.bindModelIntoEnv(model,stubs,cfg.customParams.toMap)

        env.backtest()

        reportWriter.write(model, cfg, cfg.reportTargetPath)

        return env
    }

}


