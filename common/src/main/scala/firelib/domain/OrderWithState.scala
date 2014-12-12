package firelib.domain

import firelib.common.misc.SubTopic
import firelib.common.{Order, OrderStatus, Trade}

import scala.collection.mutable.ArrayBuffer

class OrderWithState(val order : Order) {

    def status = statuses.last
    val trades = new ArrayBuffer[Trade]()
    val statuses = OrderStatus.New +: new ArrayBuffer[OrderStatus]()

    def remainingQty : Int = order.qty - trades.map(_.qty).sum

    var tradeSubscription : SubTopic[Trade] =_

    var orderSubscription : SubTopic[OrderState] =_

}
