package firelib.common.marketstub

import firelib.common.agenda.Agenda
import firelib.common.misc.{DurableTopic, Topic}
import firelib.common.timeservice.TimeService
import firelib.common.{Order, Trade}
import firelib.domain.OrderState

class TradeGateDelay(val timeService: TimeService, val delayMillis: Long, val tradeGate: TradeGate, val agenda: Agenda) extends TradeGate {
    /**
         * just order send
         */
    override def sendOrder(order: Order): (Topic[Trade], Topic[OrderState]) = {
        val trdS = new DurableTopic[Trade]()
        val ordS = new DurableTopic[OrderState]()

        agenda.addEvent(timeService.currentTime.plusMillis(delayMillis), () => {
            val sss: (Topic[Trade], Topic[OrderState]) = tradeGate.sendOrder(order)
            sss._1.subscribe(t=>trdS.publish(t))
            sss._2.subscribe(o=>ordS.publish(o))
        })
        (trdS, ordS)
    }

    /**
     * just order cancel
    */
    override def cancelOrder(order: Order): Unit = {
        agenda.addEvent(timeService.currentTime.plusMillis(delayMillis), () => {
            tradeGate.cancelOrder(order)
        })
    }
}