package firelib.common.marketstub

import java.time.Instant

import firelib.common.{TradeGateCallback, _}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class MarketStubImpl(val security: String, val maxOrderCount: Int = 40) extends MarketStub {

    var bid = Double.NaN

    var ask = Double.NaN

    var dtGmt: Instant  =_

    val id2Order = new mutable.HashMap[String, Order]()

    def orders = id2Order.values.toArray[Order]

    val tradeGateCallbacks = ArrayBuffer[TradeGateCallback]()

    var position_ = 0

    val trades = new ListBuffer[Trade]()

    var orderIdCnt = 0

    var lastPositionTrade: Trade = _

    private val delayedEvents = new ArrayBuffer[()=>Unit]()

    override def hasPendingState() : Boolean = !delayedEvents.isEmpty


    private def middlePrice: Double = (bid + ask) / 2




    def addCallback(callback: TradeGateCallback) = tradeGateCallbacks += callback


    def moveCallbacksTo(marketStub: MarketStub) : Unit = {
        tradeGateCallbacks.foreach(marketStub.addCallback)
        tradeGateCallbacks.clear()
    }

    def flattenAll(reason: Option[String]) : Unit = {
        cancelAllOrders()
        closePosition(reason)
    }

    def cancelAllOrders()  : Unit = {
        val ords = id2Order.values.toArray
        id2Order.clear()
        ords.foreach(o=>fireOrderStateDelayed(o,OrderStatus.Cancelled))

    }

    def cancelOrderByIds(orderIds: String*)  : Unit = {
        orderIds.foreach(orderId => {
            val ord = id2Order.remove(orderId)
            assert(ord.isDefined,"no order with id " + orderId)
            fireOrderStateDelayed(ord.get, OrderStatus.Cancelled)
        })
    }


    private def fireTradeEventDelayed(trade: Trade) = {
        delayedEvents += (()=>tradeGateCallbacks.foreach(tgc => tgc.onTrade(trade)))
    }

    private def fireOrderStateDelayed(order : Order, orderStatus: OrderStatus) = {
        delayedEvents += (()=>fireOrderState(order, orderStatus))
    }

    private def fireOrderState(order : Order, orderStatus: OrderStatus) = {
        order.statuses += orderStatus
        tradeGateCallbacks.foreach(_.onOrderStatus(order, orderStatus))
    }


    def submitOrders(orders: Order*) : Unit = {

        assert(this.id2Order.size + orders.length <= maxOrderCount, "max order count exceeded")
        assert(!orders.exists(o=>id2Order.contains(o.id)), "duplicate order id - order id must be uniq")

        orders.foreach(order => {
            order.placementTime = dtGmt
            this.id2Order(order.id) = order
            fireOrderState(order, OrderStatus.New)
        })
        checkOrders()
    }

    override def nextOrderId : String = {
        orderIdCnt+= 1
        security + orderIdCnt
    }

    def closePosition(reason: Option[String]): Unit = {
        if (position_ == 0)
            return

        val ord = new Order(OrderType.Market, 0, math.abs(position_), Side.sideForAmt(-position_),security, nextOrderId);
        val trd = new Trade(math.abs(position_), checkOrderAndGetTradePrice(ord).get, ord.side, ord, dtGmt) {
            reason = reason
        }
        onTrade(trd)

        assert(position_ == 0,"position must be 0 after flatten all!!!")
    }

    def updateBidAskAndTime(bid: Double, ask: Double, dtGmt:Instant) = {
        this.bid = bid
        this.ask = ask
        if (position_ != 0) {
            lastPositionTrade.onPrice(if (lastPositionTrade.side == Side.Buy) bid else ask)
        }
        this.dtGmt = dtGmt
        checkOrders()
        playEvents()
    }

    def checkOrders() : Unit = {
        id2Order retain ((id, ord) => !checkExecutionWithStateTrigger(ord).isFinal)
    }

    private def playEvents() = {
        val funcs: Array[() => Unit] = delayedEvents.toArray
        delayedEvents.clear()
        funcs.foreach(_())
    }

    private def checkExecutionWithStateTrigger(ord: Order) : OrderStatus = {
        chkOrderExecution(ord) match {
            case Some(trd) => {
                fireOrderStateDelayed(ord, OrderStatus.Done)
                return OrderStatus.Done
            }
            case None => {
                assert(ord.orderType != OrderType.Market, "market order should cause position change!!")
                //FIXME
                return OrderStatus.Accepted
            }
        }
    }


    private def chkOrderExecution(ord: Order): Option[Trade] = {
        checkOrderAndGetTradePrice(ord) match {
            case None => None
            case Some(price)=>{
                val trd = new Trade(ord.qty, price, ord.side, ord, dtGmt)
                onTrade(trd)
                Some(trd);
            }
        }
    }


    private def onTrade(trd: Trade) = {
        trades += trd
        val posBefore = position_
        position_ = trd.adjustPositionByThisTrade(position_)
        trd.positionAfter = position
        trd.order.trades += trd

        if (math.signum(posBefore) != math.signum(position)) {
            if (position == 0) {
                lastPositionTrade = null
            }
            else {
                lastPositionTrade = trd
            }
        }
        fireTradeEventDelayed(trd)
    }

    def checkOrderAndGetTradePrice(ord: Order): Option[Double] = {

        (ord.orderType, ord.side) match {
            case (OrderType.Market,Side.Buy) => Some(ask)
            case (OrderType.Market,Side.Sell) => Some(bid)

            case (OrderType.Stop,Side.Buy) if middlePrice > ord.price => Some(ask)
            case (OrderType.Stop,Side.Sell) if middlePrice < ord.price=> Some(bid)

            case (OrderType.Limit,Side.Buy) if ask < ord.price => Some(ord.price)
            case (OrderType.Limit,Side.Sell) if bid > ord.price=> Some(ord.price)
            case _ => None
        }

    }

    override def position: Int = position_


}
