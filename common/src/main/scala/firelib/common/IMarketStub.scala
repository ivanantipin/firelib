package firelib.common

import java.time.Instant

trait IMarketStub {


    def Position: Int

    /*
     * this is confirmed position + position for pending market orders
     */
    //val UnconfirmedPosition: Int

    /**
     * any market order on market or not accepted limit order
     */
    def HasPendingState: Boolean

    /**
     * name of security as configured in model config tickers list
     */
    val Security: String

    def SubmitOrders(orders: Seq[Order])

    def FlattenAll(reason: String = null)

    def trades: Seq[Trade]

    def CancelOrders

    def orders: Seq[Order]

    def CancelOrderByIds(orderIds: Seq[String]);

    def AddCallback(callback: ITradeGateCallback);

    def RemoveCallbacksTo(marketStub: IMarketStub);

    def UpdateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant);
}
