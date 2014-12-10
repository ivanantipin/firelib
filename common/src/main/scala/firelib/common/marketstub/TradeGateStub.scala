package firelib.common.marketstub

import java.util
import java.util.Comparator
import java.util.function.{Function, ToDoubleFunction, ToIntFunction, ToLongFunction}

import firelib.common.misc.{DurableTopic, Topic}
import firelib.common.timeservice.TimeService
import firelib.common.{Order, OrderStatus, Side, Trade}
import firelib.domain.OrderState



class NormComparator extends Comparator[OrderKey]{
    override def compare(o1: OrderKey, o2: OrderKey): Int = {
        if(o1.price == o2.price){
            o1.id.compareTo(o2.id)
        }else{
            o1.price.compareTo(o2.price)
        }
    }
    override def reversed(): Comparator[OrderKey] = super.reversed()
    override def thenComparingDouble(keyExtractor: ToDoubleFunction[_ >: OrderKey]): Comparator[OrderKey] = super.thenComparingDouble(keyExtractor)
    override def thenComparingInt(keyExtractor: ToIntFunction[_ >: OrderKey]): Comparator[OrderKey] = ???
    override def thenComparing(other: Comparator[_ >: OrderKey]): Comparator[OrderKey] = ???
    override def thenComparing[U](keyExtractor: Function[_ >: OrderKey, _ <: U], keyComparator: Comparator[_ >: U]): Comparator[OrderKey] = ???
    override def thenComparingLong(keyExtractor: ToLongFunction[_ >: OrderKey]): Comparator[OrderKey] = ???
}

case class OrderKey(price : Long, id : String)

case class OrderRec (val order : Order, val trdSubject : Topic[Trade], val ordSubject : Topic[OrderState])



abstract class BookStub(val timeService : TimeService) {

    protected var bid,ask = Double.NaN

    def sellOrdering  : Comparator[OrderKey]
    def buyOrdering  : Comparator[OrderKey]

    def buyMatch(bid : Double, ask : Double, ordPrice : Double) : Boolean
    def sellMatch(bid : Double, ask : Double, ordPrice : Double) : Boolean

    def buyPrice(bid : Double, ask : Double, ordPrice : Double) : Double
    def sellPrice(bid : Double, ask : Double, ordPrice : Double) : Double


    def keyForOrder(order : Order) : OrderKey = new OrderKey(order.longPrice, order.id)

    val buyOrders = new util.TreeMap[OrderKey,OrderRec](buyOrdering)

    val sellOrders = new util.TreeMap[OrderKey,OrderRec](sellOrdering)

    /**
     * just order send
     */
    def sendOrder(order: Order): (Topic[Trade],Topic[OrderState]) = {
        val el = OrderRec (order, new DurableTopic[Trade](),new DurableTopic[OrderState]())
        val ords = if(order.side == Side.Buy) buyOrders else sellOrders
        ords.put(keyForOrder(order),el)
        checkOrders()
        (el.trdSubject,el.ordSubject)
    }


    /**
     * just order cancel
     */
    def cancelOrder(order : Order): Unit = {
        val ords = if(order.side == Side.Buy) buyOrders else sellOrders
        val rec: OrderRec = ords.remove(keyForOrder(order))
        rec.ordSubject.publish(new OrderState(order,OrderStatus.Cancelled,timeService.currentTime))
    }

    def updateBidAsk(bid: Double, ask: Double) = {
        this.bid = bid
        this.ask = ask
        checkOrders()
    }

    def checkOrders() : Unit = {
        checkOrders(buyOrders,buyMatch(bid,ask,_),buyPrice(bid,ask,_))
        checkOrders(sellOrders,sellMatch(bid,ask,_),sellPrice(bid,ask,_))
    }

    def checkOrders(ords : util.TreeMap[OrderKey,OrderRec], matchFunc : Double=>Boolean, priceFunc : Double=>Double) : Unit = {
        val iter = ords.entrySet().iterator()
        var flag = true
        while(iter.hasNext && flag){
            val rec = iter.next().getValue
            if(matchFunc(rec.order.price)){
                iter.remove()
                rec.trdSubject.publish(new Trade(rec.order.qty, priceFunc(rec.order.price), rec.order, timeService.currentTime))
                rec.ordSubject.publish(new OrderState(rec.order, OrderStatus.Done, timeService.currentTime))
            }else{
                flag = false
            }
        }
    }
}

trait StopOBook {
    def sellOrdering  : Comparator[OrderKey] = new NormComparator().reversed()
    def buyOrdering  : Comparator[OrderKey] = new NormComparator

    def buyMatch(bid : Double, ask : Double, ordPrice : Double) = ordPrice < (bid + ask)/2
    def sellMatch(bid : Double, ask : Double, ordPrice : Double) = ordPrice > (bid + ask)/2

    def buyPrice(bid : Double, ask : Double, ordPrice : Double) = ask
    def sellPrice(bid : Double, ask : Double, ordPrice : Double) = bid

}

trait LimitOBook {
    def sellOrdering  : Comparator[OrderKey] = new NormComparator()
    def buyOrdering  : Comparator[OrderKey] = new NormComparator().reversed()

    def buyMatch(bid : Double, ask : Double, ordPrice : Double) = ordPrice >= ask
    def sellMatch(bid : Double, ask : Double, ordPrice : Double) = ordPrice <= bid

    def buyPrice(bid : Double, ask : Double, ordPrice : Double) = ordPrice
    def sellPrice(bid : Double, ask : Double, ordPrice : Double) = ordPrice

}



class MarketOrderStub(val timeService : TimeService) {

    protected var bid,ask = Double.NaN

    def price(side : Side) : Double = if(side == Side.Sell) bid else ask

    /**
     * just order send
     */
    def sendOrder(order: Order): (Topic[Trade],Topic[OrderState]) = {
        val ret = (new DurableTopic[Trade](),new DurableTopic[OrderState]())
        ret._1.publish(new Trade(order.qty, price(order.side), order, timeService.currentTime))
        ret._2.publish(new OrderState(order, OrderStatus.Done, timeService.currentTime))
        ret
    }


    def updateBidAsk(bid: Double, ask: Double) = {
        this.bid = bid
        this.ask = ask
    }
}
