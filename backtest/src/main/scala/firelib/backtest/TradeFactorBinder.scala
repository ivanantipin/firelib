package firelib.backtest

import firelib.common._

import scala.collection.mutable.ArrayBuffer

class TradeFactorBinder(val stubs: IMarketStub*) extends ITradeGateCallback {
    private val tradeFactors = new ArrayBuffer[IFactor]()

    stubs.foreach(_.addCallback(this))

    override def onTrade(trade: Trade): Unit = {
        tradeFactors.foreach(f => trade.addFactor(f.name, f.value))
    }

    override def onOrderStatus(order: Order, status: OrderStatus): Unit = ???

    def addFactor(that: IFactor): TradeFactorBinder = {
        tradeFactors += that
        return this
    }

}
