package firelib.common.marketstub

import java.time.Instant

import firelib.common.{Order, Trade, TradeGateCallback}


trait BidAskUpdatable{
    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant)
}

/**

 */
trait OrderManager {

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

    def trades: Seq[Trade]

    def liveOrders: Seq[Order]

    def cancelOrderByIds(orderIds : String*)

    def addCallback(callback: TradeGateCallback)

    def nextOrderId : String

    def replaceTradeGate(tg : TradeGate): Unit
}
