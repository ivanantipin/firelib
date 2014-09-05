package firelib.common

import firelib.common._

class TradeGateCallbackAdapter(val newTradeEvent: Trade => Unit = null, val newOrderEvent: Order => Unit = null) extends TradeGateCallback {

    def onTrade(trade: Trade) = {
        if (newTradeEvent != null)
            newTradeEvent(trade)
    }

    def onOrderStatus(order: Order, status: OrderStatus) {
        if (status == OrderStatus.New && newOrderEvent != null) {
            newOrderEvent(order)
        }
    }
}
