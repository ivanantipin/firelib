package firelib.common.model

import firelib.common.{Order, OrderType, Side}

trait WithTradeUtils {

    this : Model=>

    protected def managePosTo(pos: Int, idx: Int = 0): Unit = {
        getOrderForDiff(stubs(idx).position, pos) match {
            case Some(ord) => stubs(idx).submitOrders(List(ord))
            case _ =>
        }
    }

    def buyAtLimit(price: Double, vol: Int = 1, idx: Int = 0) = {
        stubs(idx).submitOrders(List(new Order(OrderType.Limit, price, vol, Side.Buy)))
    }

    def sellAtLimit(price: Double, vol: Int = 1, idx: Int = 0) = {
        stubs(idx).submitOrders(List(new Order(OrderType.Limit, price, vol, Side.Sell)))
    }

    def buyAtStop(price: Double, vol: Int = 1, idx: Int = 0) = {
        stubs(idx).submitOrders(List(new Order(OrderType.Stop, price, vol, Side.Buy)))
    }

    def sellAtStop(price: Double, vol: Int = 1, idx: Int = 0) = {
        stubs(idx).submitOrders(List(new Order(OrderType.Stop, price, vol, Side.Sell)))
    }

    def getOrderForDiff(currentPosition: Int, targetPos: Int): Option[Order] = {
        val vol = targetPos - currentPosition
        if (vol != 0) {
            return Some(new Order(OrderType.Market, 0, math.abs(vol), if (vol > 0) Side.Buy else Side.Sell))
        }
        return None
    }

    protected def flattenAll(reason: Option[String] = None) = stubs.foreach(_.flattenAll(reason))

    protected def cancelAllOrders() = stubs.foreach(_.cancelAllOrders)


}
