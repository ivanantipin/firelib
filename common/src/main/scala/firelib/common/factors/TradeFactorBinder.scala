package firelib.common.factors

import firelib.common.{TradeGateCallback, _}
import firelib.common.marketstub.MarketStub

import scala.collection.mutable.ArrayBuffer

/**
 *
 * binds factors to trades
 * so once factor added to binder it will be added to all trades automatically
 * TODO need to implement instrument wise factor
 */
class TradeFactorBinder(val stubs: MarketStub*) extends TradeGateCallback {
    private val tradeFactors = new ArrayBuffer[Factor]()

    stubs.foreach(_.addCallback(this))

    override def onTrade(trade: Trade): Unit = {
        tradeFactors.foreach(f => trade.addFactor(f.name, f.value))
    }

    override def onOrderStatus(order: Order, status: OrderStatus) = {}

    def addFactor(that: Factor): TradeFactorBinder = {
        tradeFactors += that
        return this
    }

}
