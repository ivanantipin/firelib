package firelib.common

import org.joda.time.DateTime

import scala.collection.mutable

class Trade(var Qty: Int, var Price: Double, val TradeSide: Side, val SrcOrder: Order, val DtGmt: DateTime, val Security: String) {

    assert(Qty > 0,"amount can't be negative")

    var Factors: collection.mutable.Map[String, String] = _

    var placementTime: DateTime = _;

    var PositionAfter: Int = _

    var maxHoldingPrice: Double = Price
    var minHoldingPrice: Double = Price

    var reason: String = _


    def MAE: Double = {
        return if (TradeSide == Side.Sell) Price - maxHoldingPrice else minHoldingPrice - Price
    }

    def MFE: Double = {
        return if (TradeSide == Side.Sell) Price - minHoldingPrice else maxHoldingPrice - Price
    }


    def OnPrice(pr: Double) = {
        minHoldingPrice = math.min(pr, minHoldingPrice);
        maxHoldingPrice = math.max(pr, maxHoldingPrice);
    }

    def AddFactor(name: String, value: String) {
        if (Factors == null) {
            Factors = new mutable.HashMap[String, String]()
        }
        Factors(name) = value
    }

    def AdjustPositionByThisTrade(position: Int): Int = {
        return position + TradeSide.sign * Qty
    }


    def Split(amt: Int): (Trade, Trade) = {
        var amtini = Qty - amt;

        assert(amtini >= 0,"negative amount")

        val item1 = new Trade(amt, Price, TradeSide, SrcOrder, DtGmt, Security) {
            placementTime = placementTime
            reason = reason
        }
        val item2 = if (amtini > 0)
            new Trade(amtini, Price, TradeSide, SrcOrder, DtGmt, Security) {
                placementTime = placementTime
                reason = reason
            }
        else null

        item1.minHoldingPrice = minHoldingPrice;
        item1.maxHoldingPrice = maxHoldingPrice;

        if (item2 != null) {
            item2.minHoldingPrice = minHoldingPrice;
            item2.maxHoldingPrice = maxHoldingPrice;
        }
        if (Factors != null) {
            item1.Factors = collection.mutable.Map() ++ Factors
            if (item2 != null) {
                item2.Factors = collection.mutable.Map() ++ Factors
            }
        }
        return (item1, item2)
    }

    override def toString: String = {
        return "T(%s@%s/%s/%s/Id:%s/%s/%s)" format(Utils.Dbl2Str(Price, 2), Qty, TradeSide, DtGmt, reason, if (SrcOrder != null) SrcOrder.Id else "NA", Security);
    }

}
