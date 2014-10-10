package firelib.common.model

import firelib.common.marketstub.MarketStub
import firelib.common.{Order, OrderType, Side}

trait WithTradeUtils {

    this : Model=>

    protected def managePosTo(pos: Int, idx: Int = 0): Unit = {
        getOrderForDiff(stubs(idx).position, pos, idx) match {
            case Some(ord) => stubs(idx).submitOrders(ord)
            case _ =>
        }
    }

    def buyAtLimit(price: Double, vol: Int = 1, idx: Int = 0) = {
        val stub : MarketStub = stubs(idx)
        stub.submitOrders(new Order(OrderType.Limit, price, vol, Side.Buy,stub.security,stub.nextOrderId))
    }

    def sellAtLimit(price: Double, vol: Int = 1, idx: Int = 0) = {
        val stub : MarketStub = stubs(idx)
        stubs(idx).submitOrders(new Order(OrderType.Limit, price, vol, Side.Sell,stub.security,stub.nextOrderId))
    }

    def buyAtStop(price: Double, vol: Int = 1, idx: Int = 0) = {
        val stub : MarketStub = stubs(idx)
        stubs(idx).submitOrders(new Order(OrderType.Stop, price, vol, Side.Buy,stub.security,stub.nextOrderId))
    }

    def sellAtStop(price: Double, vol: Int = 1, idx: Int = 0) = {
        val stub : MarketStub = stubs(idx)
        stubs(idx).submitOrders(new Order(OrderType.Stop, price, vol, Side.Sell,stub.security,stub.nextOrderId))
    }

    def getOrderForDiff(currentPosition: Int, targetPos: Int, idx : Int): Option[Order] = {
        val stub : MarketStub = stubs(idx)
        val vol = targetPos - currentPosition
        if (vol != 0) {
            return Some(new Order(OrderType.Market, 0, math.abs(vol), if (vol > 0) Side.Buy else Side.Sell,stub.security,stub.nextOrderId))
        }
        return None
    }

    protected def flattenAll(reason: Option[String] = None) = stubs.foreach(_.flattenAll(reason))

    protected def cancelAllOrders() = stubs.foreach(_.cancelAllOrders)


}
