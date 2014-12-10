package firelib.common.core

import firelib.common.marketstub.OrderManagerImpl
import firelib.common.mddistributor.MarketDataDistributorComponent
import firelib.common.model.{BasketModel, Model}
import firelib.common.timeservice.TimeServiceComponent
import firelib.common.tradegate.TradeGateComponent

import scala.collection.mutable.ArrayBuffer

trait BindModelComponent {

    this: ModelConfigContext
      with MarketDataDistributorComponent
      with TimeServiceComponent
      with TradeGateComponent =>

    val models = new ArrayBuffer[Model]

    def bindModelForParams(params: Map[String, String]): Model = {
        val stubs = modelConfig.instruments.map(ins => new OrderManagerImpl(this, ins.ticker))
        val model = modelConfig.newModelInstance().asInstanceOf[BasketModel]
        model.orderManagersFld = stubs.toArray
        model.bindComp = this
        if (model.initModel(params)) {
            models += model
        }
        return model
    }

}
