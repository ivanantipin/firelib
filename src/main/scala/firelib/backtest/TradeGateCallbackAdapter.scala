package firelib.backtest

import firelib.domain.{Order, OrderStatus, Trade}
import firelib.robot.ITradeGateCallback

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
