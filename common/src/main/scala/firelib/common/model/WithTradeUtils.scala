package firelib.common.model

import firelib.common.marketstub.OrderManager
import firelib.common.{Order, OrderType, Side}

object withTradeUtils {

    implicit class WithTradeUtils(val orderManager : OrderManager){
        
        def managePosTo(pos: Int): Unit = {
            getOrderForDiff(orderManager.position, pos) match {
                case Some(ord) => orderManager.submitOrders(ord)
                case _ =>
            }
        }

        def buyAtLimit(price: Double, vol: Int = 1) = {
            orderManager.submitOrders(new Order(OrderType.Limit, price, vol, Side.Buy, orderManager.security, orderManager.nextOrderId))
        }

        def sellAtLimit(price: Double, vol: Int = 1) = {
            orderManager.submitOrders(new Order(OrderType.Limit, price, vol, Side.Sell, orderManager.security, orderManager.nextOrderId))
        }

        def buyAtStop(price: Double, vol: Int = 1) = {
            orderManager.submitOrders(new Order(OrderType.Stop, price, vol, Side.Buy, orderManager.security, orderManager.nextOrderId))
        }

        def sellAtStop(price: Double, vol: Int = 1) = {
            orderManager.submitOrders(new Order(OrderType.Stop, price, vol, Side.Sell, orderManager.security, orderManager.nextOrderId))
        }

        def getOrderForDiff(currentPosition: Int, targetPos: Int): Option[Order] = {
            val vol = targetPos - currentPosition
            if (vol != 0) {
                return Some(new Order(OrderType.Market, 0, math.abs(vol), if (vol > 0) Side.Buy else Side.Sell, orderManager.security, orderManager.nextOrderId))
            }
            return None
        }

        def flattenAll(reason: Option[String] = None) = {
            cancelAllOrders()
            managePosTo(0)
        }

        def cancelAllOrders() = {orderManager.cancelOrderByIds(orderManager.liveOrders.map(_.id) :_*)}
        
    } 



}
