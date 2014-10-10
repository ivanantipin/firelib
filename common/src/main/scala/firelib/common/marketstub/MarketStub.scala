package firelib.common.marketstub

import java.time.Instant

import firelib.common.{Order, Trade, TradeGateCallback}

/**

 */
trait MarketStub {

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

    def flattenAll(reason: Option[String])

    def trades: Seq[Trade]

    def cancelAllOrders()

    def orders: Iterable[Order]

    def cancelOrderByIds(orderIds : String*)

    def addCallback(callback: TradeGateCallback)

    def moveCallbacksTo(marketStub: MarketStub)

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant)

    def nextOrderId : String
}
