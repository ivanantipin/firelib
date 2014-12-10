package firelib.common.marketstub

import firelib.common._
import firelib.common.misc.{NonDurableTopic, Topic}
import firelib.common.timeservice.TimeServiceComponent
import firelib.common.tradegate.TradeGateComponent
import firelib.domain.{OrderState, OrderWithState}
import org.slf4j.LoggerFactory

import scala.collection.mutable




class OrderManagerImpl(val bindComp : TradeGateComponent with TimeServiceComponent , val security : String, val maxOrderCount: Int = 20) extends OrderManager {

    private val id2Order = new mutable.HashMap[String,OrderWithState]()

    private var position_ = 0

    private var orderIdCnt = 0

    private val log = LoggerFactory.getLogger(getClass)

    override def position: Int = position_

    var idCounter = System.currentTimeMillis()

    def liveOrders: Seq[Order] = id2Order.values.map(_.order).to[Seq]

    def tradeGate = bindComp.tradeGate

    def currentTime = bindComp.timeService.currentTime

    override def hasPendingState: Boolean = {
        id2Order.values.exists(o => (o.status.isPending || (o.order.orderType == OrderType.Market)))
    }

    private val tradesSubject = new NonDurableTopic[Trade]()
    private val ordersSubject = new NonDurableTopic[OrderState]()

    override def listenTrades(lsn: Trade=>Unit) = tradesSubject.subscribe(lsn)

    override def listenOrders(lsn: OrderState=>Unit) = ordersSubject.subscribe(lsn)

    def cancelOrders(orders: Order*): Unit = {
        for (order <- orders) {
            id2Order.get(order.id) match {
                case Some(ord) => {
                    tradeGate.cancelOrder(order)
                    ord.statuses += OrderStatus.PendingCancel
                    ordersSubject.publish(new OrderState(ord.order,OrderStatus.PendingCancel, currentTime))
                }

                case None => log.error("cancelling non existing order {}", order)
            }
        }
    }


    def submitOrders(orders: Order*) = {
        if(this.id2Order.size > maxOrderCount){
            log.error("max order count reached rejecting orders {}", orders)
            orders.foreach(ord=>ordersSubject.publish(new OrderState(ord,OrderStatus.Rejected, currentTime)))
        }else{
            orders.foreach(order => {
                val orderWithState: OrderWithState = new OrderWithState(order)
                this.id2Order(order.id) = orderWithState
                orders.foreach(ord=>ordersSubject.publish(new OrderState(ord,OrderStatus.New, currentTime)))
                log.info("submitting order {}", order)
                val obss: (Topic[Trade], Topic[OrderState]) = tradeGate.sendOrder(order)
                orderWithState.tradeSubscription = obss._1
                orderWithState.orderSubscription = obss._2
                orderWithState.tradeSubscription.subscribe((t : Trade) =>onTrade(t, orderWithState))
                orderWithState.orderSubscription.subscribe(onOrderState)
            })

        }
    }



    def onOrderState(state : OrderState): Unit = {
        if (!id2Order.contains(state.order.id)) {
            log.error("order state received {} for nonexisting or finalized order", state)
            return
        }
        log.info("order state received {} ", state)

        val sOrder: OrderWithState = id2Order(state.order.id)

        sOrder.statuses += state.status

        if (state.status.isFinal) {
            if(state.status == OrderStatus.Done && sOrder.remainingQty > 0){
                log.error("status is Done but order {} has non zero remaining amount {} ", state.order, sOrder.remainingQty)
            }
            val finalOrder: OrderWithState = id2Order.remove(state.order.id).get
        }
        ordersSubject.publish(state)
    }


    def onTrade(trd: Trade, order : OrderWithState): Unit = {
        log.info("on trade {}", trd)
        order.trades += trd
        if(order.remainingQty < 0){
            log.error("negative remaining amount order {}", order)
        }
        val prevPos = position_
        position_ = trd.adjustPositionByThisTrade(position_)
        trd.positionAfter = position_
        if(log.isInfoEnabled()) log.info(s"position adjusted for security $security :  $prevPos -> $position")
        tradesSubject.publish(trd)
    }


    override def nextOrderId: String = {
        idCounter+= 1
        return s"${security}_${idCounter}"
    }
}
