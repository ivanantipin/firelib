package firelib.common

import java.time.Instant

trait IMarketStub {


    def position: Int

    /*
     * this is confirmed position + position for pending market orders
     */
    //val UnconfirmedPosition: Int

    /**
     * any market order on market or not accepted limit order
     */
    def hasPendingState(): Boolean

    /**
     * name of security as configured in model config tickers list
     */
    val security: String

    def submitOrders(orders: Seq[Order])

    def flattenAll(reason: String = null)

    def trades: Seq[Trade]

    def cancelAllOrders()

    def orders: Seq[Order]

    def cancelOrderByIds(orderIds: Seq[String])

    def addCallback(callback: ITradeGateCallback)

    def moveCallbacksTo(marketStub: IMarketStub)

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant)
}
