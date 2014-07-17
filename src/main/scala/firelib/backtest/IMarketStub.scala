package firelib.backtest

import firelib.domain.{Order, Trade}
import firelib.robot.ITradeGateCallback
import org.joda.time.DateTime

trait IMarketStub {


    val Position: Int

    /*
     * this is confirmed position + position for pending market orders
     */
    val UnconfirmedPosition: Int

    /**
     * any market order on market or not accepted limit order
     */
    var HasPendingState: Int

    /**
     * name of security as configured in model config tickers list
     */
    val Security: String

    def SubmitOrders(orders: Order*)

    def FlattenAll(reason: String = null)

    def trades: Seq[Trade]

    def CancelOrders

    def orders: Seq[Order]

    def CancelOrderByIds(orderIds: Array[String]);

    def AddCallback(callback: ITradeGateCallback);

    def RemoveCallbacksTo(marketStub: IMarketStub);

    def UpdateBidAskAndTime(bid: Double, ask: Double, dtGmt: DateTime);
}
