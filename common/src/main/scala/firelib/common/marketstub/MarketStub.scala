package firelib.common.marketstub

import java.time.Instant

import firelib.common.TradeGateCallback
import firelib.common.{Order, Trade}

/**
 * Created by ivan on 9/5/14.
 */
trait MarketStub {


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

    def flattenAll(reason: Option[String])

    def trades: Seq[Trade]

    def cancelAllOrders()

    def orders: Seq[Order]

    def cancelOrderByIds(orderIds: Seq[String])

    def addCallback(callback: TradeGateCallback)

    def moveCallbacksTo(marketStub: MarketStub)

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant)
}
