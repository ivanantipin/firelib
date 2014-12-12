package firelib.common.tradegate

import firelib.common.misc.{DurableTopic, Topic}
import firelib.common.timeservice.TimeService
import firelib.common.{Order, OrderStatus, Side, Trade}
import firelib.domain.OrderState


class MarketOrderStub(val timeService : TimeService) {

    protected var bid,ask = Double.NaN

    def price(side : Side) : Double = if(side == Side.Sell) bid else ask

    /**
     * just order send
     */
    def sendOrder(order: Order): (Topic[Trade],Topic[OrderState]) = {
        val ret = (new DurableTopic[Trade](),new DurableTopic[OrderState]())
        val trdPrice: Double = price(order.side)
        if(trdPrice.isNaN){
            ret._2.publish(new OrderState(order, OrderStatus.Rejected, timeService.currentTime))
        }else{
            ret._1.publish(new Trade(order.qty, trdPrice, order, timeService.currentTime))
            ret._2.publish(new OrderState(order, OrderStatus.Done, timeService.currentTime))
        }
        ret
    }


    def updateBidAsk(bid: Double, ask: Double) = {
        this.bid = bid
        this.ask = ask
    }
}
