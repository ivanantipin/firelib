package firelib.common.core

import firelib.common.marketstub.{BidAskUpdater, MarketStub, MarketStubFactoryComponent}
import firelib.common.mddistributor.MarketDataDistributorComponent
import firelib.common.model.Model

import scala.collection.mutable.ArrayBuffer

trait BindModelComponent{

    this : ModelConfigContext
      with MarketDataDistributorComponent
      with MarketStubFactoryComponent
      with StepServiceComponent =>

    val models = new ArrayBuffer[Model]

    def bindModelForParams(params: Map[String, String]): Model = {
        val stubs: ArrayBuffer[MarketStub] = modelConfig.instruments.map(marketStubFactory)
        val model: Model = modelConfig.newModelInstance()
        model.initModel(params, stubs, marketDataDistributor)
        if (model.hasValidProps()) {
            stepService.priorityListen(model)
            val updater = new BidAskUpdater(stubs)
            marketDataDistributor.addMdListener(updater)
            stepService.listen(updater)
            models += model
        }
        return model
    }

}
