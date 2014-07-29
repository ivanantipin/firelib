package firelib.backtest

import firelib.common._

import scala.collection.mutable.ArrayBuffer

class TradeFactorBinder(val stubs: IMarketStub*) extends ITradeGateCallback {
    private val tradeFactors = new ArrayBuffer[IFactor]();

    stubs.foreach(_.AddCallback(this))

    override def OnTrade(trade: Trade): Unit = {
        tradeFactors.foreach(f => trade.AddFactor(f.Name, f.Value))
    }

    override def OnOrderStatus(order: Order, status: OrderStatus): Unit = ???

    def +(that: IFactor): TradeFactorBinder = {
        tradeFactors += that
        return this;
    }

}
