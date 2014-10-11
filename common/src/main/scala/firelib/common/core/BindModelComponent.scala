package firelib.common.core

import firelib.common.marketstub.{BidAskUpdatable, BidAskUpdater, OrderManager, OrderManagerImpl, TradeGateStub}
import firelib.common.mddistributor.MarketDataDistributorComponent
import firelib.common.model.Model

import scala.collection.mutable.ArrayBuffer

trait BindModelComponent{

    this : ModelConfigContext
      with MarketDataDistributorComponent
      with StepServiceComponent =>

    val models = new ArrayBuffer[Model]

    def bindModelForParams(params: Map[String, String]): Model = {

        val stubs: ArrayBuffer[(OrderManager, BidAskUpdatable)] = modelConfig.instruments.map(ins=>{
            val tg: TradeGateStub = new TradeGateStub
            val om = new OrderManagerImpl(tg, ins.ticker)
            (om,tg)
        })

        val model: Model = modelConfig.newModelInstance()
        model.initModel(params, stubs.map(_._1), marketDataDistributor)
        if (model.hasValidProps()) {
            stepService.priorityListen(model)
            val updater = new BidAskUpdater(stubs.map(_._2))
            marketDataDistributor.addMdListener(updater)
            stepService.listen(updater)
            models += model
        }
        return model
    }

}
