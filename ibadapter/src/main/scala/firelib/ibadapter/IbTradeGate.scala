package firelib.ibadapter

import java.time.Instant
import java.util
import java.util.Properties
import java.util.concurrent.{Executors, LinkedBlockingQueue, TimeUnit}

import com.ib.client
import com.ib.client.{Contract, Execution, TagValue}
import firelib.common._
import firelib.common.misc.{Channel, DurableChannel}
import firelib.common.threading.ThreadExecutor
import firelib.common.tradegate.TradeGate
import firelib.domain.{OrderState, Tick}
import firelib.execution.{Configurable, MarketDataProvider}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * adapter over ib api
 * IMPORTANT!! - all code must run in thread executor provided on creation
 *
 */
class IbTradeGate extends EWrapperImpl with TradeGate with MarketDataProvider with Configurable{
    val orders = new ArrayBuffer[OrderEntry]()

    var port: Int = _

    private var subscriptionTickerCounter = 0
    private var symbolMapping: Map[String, String] = _

    private val ticker2contract = new mutable.HashMap[String, Contract]()
    private var callbackExecutor: ThreadExecutor = _

    private val orderIdQueue = new LinkedBlockingQueue[Integer]()

    val executor = Executors.newSingleThreadScheduledExecutor()


    case class OrderEntry(fireLibOrder: Order, ibOrder: com.ib.client.Order, ibId: Int, tradeSubj : Channel[Trade] , orderSubj : Channel[OrderState])

    private def nextOrderId: Option[Int] = {
        log.info("requesting order for client id " + clientId)
        orderIdQueue.clear()
        try{
            clientSocket.reqIds(1)
        }catch {
            case ex : Throwable => return None
        }
        val ordId = orderIdQueue.poll(5, TimeUnit.SECONDS)
        if (ordId == null) {
            log.error("requested but did not receive orderId for" + clientId)
            return None
        }
        return Some(ordId)
    }


    /**
     * !!! this is called in ib thread
     * @param orderId
     */
    override def nextValidId(orderId: Int): Unit = {
        log.info("received order id " + orderId + " for client id " + clientId)
        orderIdQueue.add(orderId)
    }


    def sendOrder(order: Order): (Channel[Trade],Channel[OrderState]) = {
        log.info("sending order " + order)
        val ret = (new DurableChannel[Trade], new DurableChannel[OrderState])
        nextOrderId match{
            case Some(orderId) =>{
                val ibOrder = convertOrder(order)
                orders += new OrderEntry(order, ibOrder, orderId, ret._1, ret._2)
                clientSocket.placeOrder(orderId, parse(order.security), ibOrder)
                log.info("order placed to socket  " + order + " order id is " + orderId)
            }
            case None =>{
                log.error("failed to get next available order id, rejecting order " + order)
                ret._2.publish(new OrderState(order,OrderStatus.Rejected,Instant.now()))
            }
        }
        ret
    }

    def cancelOrder(order: Order): Unit = {
        log.info("cancelling order " + order)
        orders.find(_.fireLibOrder.id == order.id) match {
            case Some(e) => {
                clientSocket.cancelOrder(e.ibId)
                log.info("order cancel placed to socket  " + e)
            }
            case None => log.error("failed to find order for id " + order)
        }
    }

    override def start(config: Map[String, String],callbackExecutor: ThreadExecutor) = {
        val props = new Properties();
        props.load(getClass().getResourceAsStream("/contract.properties"))
        this.symbolMapping = props.toMap
        log.info(s"contracts loaded : $symbolMapping")
        port = config("port").toInt
        clientId = config("client.id").toInt
        this.callbackExecutor = callbackExecutor
        executor.scheduleAtFixedRate(new Runnable {
            override def run(): Unit = heartBeat()
        }, 8, 8, TimeUnit.HOURS)
        connect()

    }

    private def heartBeat() = {
        callbackExecutor.execute(() => {
            try {
                nextOrderId match {
                    case None =>{
                        log.error("heartbeat failed , trying to reconnect")
                        clientSocket.eDisconnect()
                        connect()
                        resubscribe()
                    }
                    case _ =>
                }
            }
            catch {
                case ex: Throwable => log.error("Failed to heartbeat due to exception", ex)
            }
        })
    }

    private def connect() = {
        clientSocket.eConnect("127.0.0.1", port, clientId)
        var cnt = 0
        while(!clientSocket.isConnected && cnt < 10){
            Thread.sleep(100)
            cnt+= 1
        }
        if(!clientSocket.isConnected){
            throw new RuntimeException("failed to connect")
        }
    }

    override def execDetails(reqId: Int, contract: Contract, execution: Execution) = {
        callbackExecutor.execute(() => {

            super.execDetails(reqId, contract, execution)

            orders.find(_.ibId == execution.m_orderId) match {
                case None => log.error("execution, no order found for ib order id " + execution.m_orderId)
                case Some(entr) => entr.tradeSubj.publish(new Trade(execution.m_shares, execution.m_price, entr.fireLibOrder,Instant.now()))
            }
        })
    }

    override def orderStatus(orderId: Int, status: String, filled: Int, remaining: Int, avgFillPrice: Double, permId: Int, parentId: Int,
                             lastFillPrice: Double, clientId: Int, whyHeld: String): Unit = {
        callbackExecutor.execute(() => {
            log.info("OrderStatus. Id: " + orderId + ", Status: " +
              status + ", Filled" + filled + ", Remaining: " + remaining
              + ", AvgFillPrice: " + avgFillPrice + ", PermId: " + permId + ", ParentId: " +
              parentId + ", LastFillPrice: " + lastFillPrice + ", ClientId: " + clientId + ", " +
              "WhyHeld: " + whyHeld + "\n")


            val entrOpt = orders.find(_.ibId == orderId)

            if (entrOpt.isEmpty) {
                log.error("no order found for ib id " + orderId)
                return
            }
            val entr = entrOpt.get

            /*
                     *      PendingSubmit - indicates that you have transmitted the order, but have not yet received confirmation that it has been accepted by the order destination. NOTE: This order status is not sent by TWS and should be explicitly set by the API developer when an order is submitted.
                     *      PendingCancel - indicates that you have sent a request to cancel the order but have not yet received cancel confirmation from the order destination. At this point, your order is not confirmed canceled. You may still receive an execution while your cancellation request is pending. NOTE: This order status is not sent by TWS and should be explicitly set by the API developer when an order is canceled.
                     *      PreSubmitted - indicates that a simulated order type has been accepted by the IB system and that this order has yet to be elected. The order is held in the IB system until the election criteria are met. At that time the order is transmitted to the order destination as specified .
                     *      Submitted - indicates that your order has been accepted at the order destination and is working.
                     *      ApiCanceled - after an order has been submitted and before it has been acknowledged, an API client client can request its cancelation, producing this state.
                     *      Cancelled - indicates that the balance of your order has been confirmed canceled by the IB system. This could occur unexpectedly when IB or the destination has rejected your order.
                     *      Filled - indicates that the order has been completely filled.
                     *      Inactive - indicates that the order has been accepted by the system (simulated orders) or an exchange (native orders) but that currently the order is inactive due to system, exchange or other issues.

             */

            status match {

                case ("PendingSubmit" | "PendingCancel") =>



                case "Inactive" =>  entr.orderSubj.publish(new OrderState(entr.fireLibOrder,OrderStatus.Rejected,Instant.now()))

                case ("PreSubmitted" | "Submitted") => entr.orderSubj.publish(new OrderState(entr.fireLibOrder,OrderStatus.Accepted,Instant.now()))

                case ("Cancelled" | "ApiCancelled" | "ApiCanceled") => entr.orderSubj.publish(new OrderState(entr.fireLibOrder,OrderStatus.Cancelled,Instant.now()))

                case "Filled" => entr.orderSubj.publish(new OrderState(entr.fireLibOrder,OrderStatus.Done,Instant.now()))

            }
        })
    }

    override def error(id: Int, errorCode: Int, errorMsg: String) = {
        log.error(s"error message $errorMsg code $errorCode id $id")
        callbackExecutor.execute(() => {
            val find = subscriptionByRequestId(id)
            if (find.isDefined) {
                log.error("failed to subscribe %s message is %s".format(find.get.TickerId, errorMsg))
            }
        })
    }


    private def convertOrder(ord: Order): client.Order = {
        val order = new com.ib.client.Order() {
            m_action = if (ord.side == Side.Buy) "BUY" else "SELL"
            m_orderType = if (ord.orderType == OrderType.Market) "MKT" else "LMT"
            m_totalQuantity = ord.qty
        }
        if (ord.orderType == OrderType.Market) {
            order.m_tif = "IOC"
        }
        return order
    }

    private def parse(ticker: String): Contract = {
        if (ticker2contract.contains(ticker)) {
            return ticker2contract(ticker)
        }

        assert(symbolMapping.contains(ticker), "no mapping for " + ticker)

        var contractSpec = symbolMapping(ticker)

        val contract = new Contract()

        for (v <- contractSpec.split(';')) {
            var mm = v.split('=')
            contract.getClass.getField(mm(0)).set(contract, mm(1))
        }
        ticker2contract(ticker) = contract
        return contract
    }

    private val subscriptions = new ArrayBuffer[Subscriptions]()
    private var clientId: Int = _


    override def tickSize(tickerId: Int, field: Int, size: Int) = {
        callbackExecutor.execute(() => {

            subscriptionByRequestId(tickerId) match {
                case Some(s) =>{
                    var pr = s.lastTick
                    pr.vol = size
                    subscriptionByRequestId(tickerId).get.listeners.foreach(_.apply(pr))
                }
                case None => {
                    log.error(s"no subscription found for tickerId=$tickerId")
                }
            }

        })
    }

    override def tickPrice(tickerId: Int, field: Int, price: Double, canAutoExecute: Int) = {
        callbackExecutor.execute(() => {
            if (price > 0 && !price.isNaN && !price.isInfinity) {
                subscriptionByRequestId(tickerId).get.lastTick = new Tick() {
                    last = price;
                    dtGmt = Instant.now()
                }
            } else {
                log.error("wrong price for " + subscriptionByRequestId(tickerId).get + " price " + price)
            }


        })
    }


    private class Subscriptions(val TickerId: String, val contract: Contract) {
        val listeners = new ArrayBuffer[Tick => Unit]()
        var lastTick: Tick = _
        var requestId: Int = -1

        def resubscribe() = {
            if (requestId != -1) clientSocket.cancelMktData(requestId)
            requestId = nextSubscriptionReqId()
            log.info(s"subscribing contract $contract requestId $requestId")
            clientSocket.reqMktData(requestId, contract, "", false, new util.ArrayList[TagValue]())
        }
    }

    private def subscriptionByRequestId(tickerId: Int): Option[Subscriptions] = {
        subscriptions.find(_.requestId == tickerId)
    }

    def nextSubscriptionReqId(): Int = {
        subscriptionTickerCounter += 2
        return subscriptionTickerCounter
    }

    private def resubscribe() {
        subscriptions.foreach(_.resubscribe())
    }

    override def subscribeForTick(tickerId: String, lsn: (Tick) => Unit): Unit = {
        callbackExecutor.execute(() => {
            val contract = parse(tickerId)
            subscriptions.find(_.TickerId == tickerId) match {
                case Some(ss) => ss.listeners += lsn
                case None => {
                    val ss: Subscriptions = new Subscriptions(tickerId, contract) {
                        listeners += lsn
                    }
                    subscriptions += ss
                    ss.resubscribe()
                }
            }
        })

    }
}
