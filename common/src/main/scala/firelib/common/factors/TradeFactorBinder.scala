package firelib.common.factors

import firelib.common._
import firelib.common.ordermanager.OrderManager

import scala.collection.mutable.ArrayBuffer

/**
 *
 * binds factors to trades
 * so once factor added to binder it will be added to all trades automatically
 * TODO need to implement instrument wise factor
 */
class TradeFactorBinder(val stubs: OrderManager*) {
    private val tradeFactors = new ArrayBuffer[Factor]()

    stubs.foreach(_.listenTrades(onTrade))

    def onTrade(trade: Trade): Unit = {
        tradeFactors.foreach(f => trade.addFactor(f.name, f.value))
    }

    def addFactor(that: Factor): TradeFactorBinder = {
        tradeFactors += that
        return this
    }

}
