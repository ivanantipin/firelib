package firelib.backtest

import firelib.common._

class TradeGateCallbackAdapter(val newTradeEvent: Trade => Unit = null, val newOrderEvent: Order => Unit = null) extends ITradeGateCallback {

    def OnTrade(trade: Trade) = {
        if (newTradeEvent != null)
            newTradeEvent(trade);
    }

    def OnOrderStatus(order: Order, status: OrderStatus) {
        if (status == OrderStatus.New && newOrderEvent != null) {
            newOrderEvent(order);
        }
    }

}
