package firelib.common.ordermanager

import java.time.Instant

import firelib.common.misc.SubTopic
import firelib.common.{Order, Trade}
import firelib.domain.OrderState


trait OrderManager extends OrderManagerUtils{

    /**
     * position
     */
    def position: Int

    /**
     * any market order on market or not accepted limit order
     */
    def hasPendingState(): Boolean

    /**
     * alias of security as configured in model config instruments list
     */
    val security: String

    def submitOrders(orders: Order*)

    def liveOrders: Seq[Order]

    val tradesTopic : SubTopic[Trade]

    val orderStateTopic : SubTopic[OrderState]

    def cancelOrders(orders: Order*)

    def nextOrderId : String

    def currentTime : Instant
}
