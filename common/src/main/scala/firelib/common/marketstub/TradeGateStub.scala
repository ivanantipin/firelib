package firelib.common.marketstub

import java.time.Instant

import firelib.common.threading.ThreadExecutor
import firelib.common.{DisposableSubscription, Order, OrderStatus, OrderType, Side, Trade, TradeGateCallback}

import scala.collection.mutable.ArrayBuffer

class TradeGateStub extends TradeGate with BidAskUpdatable{


    private var bid,ask = Double.NaN

    var dtGmt: Instant  =_

    val orders = new ArrayBuffer[Order]()

    private val delayedEvents = new ArrayBuffer[()=>Unit]()

    private def middlePrice: Double = (bid + ask) / 2


    private val tradeGateCallbacks = ArrayBuffer[TradeGateCallback]()
    /**
     * just order send
     */
    override def sendOrder(order: Order): Unit = {
        delayedEvents += (()=>{this.orders += order})
    }

    /**
     * register callback to receive notifications about trades and order statuses
     */
    override def registerCallback(tgc: TradeGateCallback): DisposableSubscription = {
        tradeGateCallbacks += tgc
        return new DisposableSubscription {
            override def unsubscribe(): Unit = tradeGateCallbacks -= tgc
        }
    }

    /**
     * pass configuration params to gate
     * usually it is user/password, broker port and url etc
     */
    override def configure(config: Map[String, String], callbackExecutor: ThreadExecutor): Unit = {}

    /**
     * just order cancel by id
     */
    override def cancelOrder(orderId: String): Unit = {
        delayedEvents += (()=>{
            val ord = orders.find(_.id == orderId)
            assert(ord.isDefined,"no order with id " + orderId)
            orders -= ord.get
            tradeGateCallbacks.foreach(_.onOrderStatus(ord.get, OrderStatus.Cancelled))
        })
    }

    /**
     * need to run start to finish initialization
     */
    override def start(): Unit = {}



    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant) = {
        this.bid = bid
        this.ask = ask
        this.dtGmt = dtGmt
        playEvents()
        checkOrders()
    }

    def checkOrders() : Unit = {
        var i = orders.size - 1;
        while(i >= 0){
            val ord = orders(i)
            checkOrderAndGetTradePrice(ord) match {
                case None =>
                case Some(price)=>{
                    tradeGateCallbacks.foreach(_.onTrade(new Trade(ord.qty, price, ord.side, ord, dtGmt)))
                    orders.remove(i)
                    tradeGateCallbacks.foreach(_.onOrderStatus(ord, OrderStatus.Done))
                }
            }
            i-=1
        }
    }

    private def playEvents() = {
        if(delayedEvents.nonEmpty){
            val funcs: Array[() => Unit] = delayedEvents.toArray
            delayedEvents.clear()
            funcs.foreach(_())
        }
    }


    def checkOrderAndGetTradePrice(ord: Order): Option[Double] = {

        (ord.orderType, ord.side) match {
            case (OrderType.Market,Side.Buy) => Some(ask)
            case (OrderType.Market,Side.Sell) => Some(bid)

            case (OrderType.Stop,Side.Buy) if middlePrice > ord.price => Some(ask)
            case (OrderType.Stop,Side.Sell) if middlePrice < ord.price=> Some(bid)

            case (OrderType.Limit,Side.Buy) if middlePrice < ord.price => Some(ord.price)
            case (OrderType.Limit,Side.Sell) if middlePrice > ord.price=> Some(ord.price)
            case _ => None
        }

    }

}
