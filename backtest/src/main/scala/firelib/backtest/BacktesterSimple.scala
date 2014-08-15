package firelib.backtest

import firelib.common._
import firelib.utils.ReportWriter

import scala.collection.mutable.ArrayBuffer


class BacktesterSimple (envFactory : BacktestEnvironmentFactory, stubFactory : MarketStubFactory) {

    def run(cfg: ModelConfig) : BacktestEnvironment = {

        val env : BacktestEnvironment = envFactory(cfg)

        val model = cfg.newInstance()

        val stubs: ArrayBuffer[IMarketStub] = cfg.tickerIds.map(stubFactory)

        env.bindModelIntoEnv(model,stubs,cfg.customParams.toMap)

        env.backtest()

        ReportWriter.write(model, cfg, cfg.reportRoot)

        return env
    }

}


