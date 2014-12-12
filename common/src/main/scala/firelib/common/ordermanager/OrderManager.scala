package firelib.common.ordermanager

import java.time.Instant

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

    def listenTrades(lsn : Trade=>Unit)

    def listenOrders(lsn : OrderState=>Unit)

    def cancelOrders(orders: Order*)

    def nextOrderId : String

    def currentTime : Instant
}
