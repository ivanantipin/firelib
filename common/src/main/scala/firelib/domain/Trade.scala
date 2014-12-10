package firelib.common

import java.time.Instant

import firelib.common.misc.utils

import scala.collection.mutable

class Trade(val qty: Int, val price: Double, val order: Order, val dtGmt:Instant) {

    assert(qty >= 0,"amount can't be negative")

    assert(order != null,"order must be present")

    var factors: collection.mutable.Map[String, String] = _

    var positionAfter: Int = _

    var maxHoldingPrice: Double = price
    var minHoldingPrice: Double = price

    def security = order.security

    def side = order.side

    def MAE: Double = {
        if (order.side == Side.Sell) price - maxHoldingPrice else minHoldingPrice - price
    }

    def MFE: Double = {
        if (order.side == Side.Sell) price - minHoldingPrice else maxHoldingPrice - price
    }


    def onPrice(pr: Double) = {
        minHoldingPrice = math.min(pr, minHoldingPrice)
        maxHoldingPrice = math.max(pr, maxHoldingPrice)
    }

    def addFactor(name: String, value: String) {
        if (factors == null) {
            factors = new mutable.HashMap[String, String]()
        }
        factors(name) = value
    }

    def adjustPositionByThisTrade(position: Int): Int = position + side.sign * qty

    def moneyFlow = - qty * price * side.sign


    def split(amt: Int): (Trade, Trade) = {
        val reminder = qty - amt
        assert(reminder >= 0,"negative amount")
        return (sameTradeForAmount(amt), sameTradeForAmount(reminder))
    }

    def sameTradeForAmount(qty : Int) : Trade = {
        val ret = new Trade(qty, price, order, dtGmt)
        ret.minHoldingPrice = minHoldingPrice
        ret.maxHoldingPrice = maxHoldingPrice
        if (factors != null) {
            ret.factors = collection.mutable.Map() ++ factors
        }
        return ret

    }

    override def toString: String = {
        s"Trade(price=${utils.dbl2Str(price, 2)} qty=$qty side=$side dtGmt=$dtGmt orderId=${order.id} sec=$security)"
    }

}
