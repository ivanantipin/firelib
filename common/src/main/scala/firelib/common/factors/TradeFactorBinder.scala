package firelib.common.factors

import firelib.common.TradeGateCallback
import firelib.common.marketstub.MarketStub
import firelib.common._

import scala.collection.mutable.ArrayBuffer

class TradeFactorBinder(val stubs: MarketStub*) extends TradeGateCallback {
    private val tradeFactors = new ArrayBuffer[Factor]()

    stubs.foreach(_.addCallback(this))

    override def onTrade(trade: Trade): Unit = {
        tradeFactors.foreach(f => trade.addFactor(f.name, f.value))
    }

    override def onOrderStatus(order: Order, status: OrderStatus): Unit = ???

    def addFactor(that: Factor): TradeFactorBinder = {
        tradeFactors += that
        return this
    }

}
