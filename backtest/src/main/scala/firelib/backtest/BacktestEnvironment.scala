package firelib.backtest

import java.time.Instant

import firelib.common.{IMarketStub, IModel, MarketDataDistributor}

import scala.collection.mutable.ArrayBuffer

class BacktestEnvironment(val player : MarketDataPlayer, val mdDistributor : MarketDataDistributor, val bounds : (Instant,Instant)){

    val models = new ArrayBuffer[IModel]()

    def backtest(){
        player.play()
        models.foreach(_.onBacktestEnd())
    }

    def bindModelIntoEnv(model : IModel, stubs : Seq[IMarketStub],  modelProps: Map[String, String]): Unit = {
        model.initModel(modelProps, stubs, mdDistributor)
        if(model.hasValidProps()){
            player.addStepListenerAtBeginning(model)
            bindStubs(stubs)
            models += model
        }
    }

    private def bindStubs(stubs : Seq[IMarketStub]): Unit = {
        val updater = new BidAskUpdater(stubs)
        player.addListener(updater)
        player.addStepListener(updater)
    }

}
