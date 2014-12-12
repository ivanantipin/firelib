package firelib.common.core

import firelib.common.Trade
import firelib.common.model.Model
import firelib.domain.OrderState

import scala.collection.mutable.ArrayBuffer

class ModelOutput(val model : Model){
    val trades = new ArrayBuffer[Trade]()
    val orderStates = new ArrayBuffer[OrderState]()
    model.orderManagers.foreach(_.listenTrades(trades += _))
    model.orderManagers.foreach(_.listenOrders(orderStates += _))
}
