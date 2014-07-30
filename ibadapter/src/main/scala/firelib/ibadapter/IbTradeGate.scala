package firelib.ibadapter

import java.time.Instant
import java.util
import java.util.concurrent.{Executors, LinkedBlockingQueue, TimeUnit}

import com.ib.client
import com.ib.client.{Contract, Execution, TagValue, TickType}
import firelib.common._
import firelib.robot.{IMarketDataProvider, ITradeGate}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class IbTradeGate extends EWrapperImpl with ITradeGate with IMarketDataProvider {
    val tradeGateCallbacks = new ArrayBuffer[ITradeGateCallback]();
    val orders = new ArrayBuffer[OrderEntry]();

    var port: Int=_

    private var subscriptionTickerCounter = 0
    private var symbolMapping: Map[String, String] =_

    private val ticker2contract = new mutable.HashMap[String, Contract]()
    private var callbackExecutor: IThreadExecutor =_

    private val orderIdQueue = new LinkedBlockingQueue[Integer]()


    case class OrderEntry(ClientOrder: Order, IbOrder: com.ib.client.Order, IbId: Int)

    private def GetNextOrderId: Int = {
        log.info("requesting order for client id " + clientId);
        orderIdQueue.clear();
        clientSocket.reqIds(1);
        val ordId = orderIdQueue.poll(5, TimeUnit.SECONDS)
        if (ordId == null) {
            log.error("requested but did not receive orderId for" + clientId)
            return -1
        }
        return ordId
    }


    //this is called in reader thread!!!
    override def nextValidId(orderId: Int) {
        log.info("received order id " + orderId + " for client id " + clientId);
        orderIdQueue.add(orderId)
    }


    def sendOrder(order: Order): Unit = {
        log.info("sending order " + order);
        val ibOrder = ConvertOrder(order);
        val orderId = GetNextOrderId;

        if (orderId == -1) {
            log.error("failed to get next available order id, rejecting order " + order);
            callbackExecutor.Execute(() => tradeGateCallbacks.foreach(_.onOrderStatus(order, OrderStatus.Rejected)));
            return;
        }

        orders += new OrderEntry(order, ibOrder, orderId)

        clientSocket.placeOrder(orderId, Parse(order.Security), ibOrder);
        log.info("order placed to socket  " + order + " order id is " + orderId);
    }

    def cancelOrder(orderId: String): Unit = {
        log.info("cancelling order " + orderId);
        orders.find(_.ClientOrder.Id == orderId) match {
            case Some(e) => {
                clientSocket.cancelOrder(e.IbId);
                log.info("order cancel placed to socket  " + e);
            }
            case None => log.error("failed to find order for id " + orderId)
        }
    }

    def registerCallback(tgc: ITradeGateCallback) = {
        tradeGateCallbacks += tgc
    }

    def configure(config: Map[String, String], symbolMapping: Map[String, String], callbackExecutor: IThreadExecutor) = {
        this.symbolMapping = symbolMapping;
        port = config("port").toInt;
        clientId = config("client.id").toInt;
        this.callbackExecutor = callbackExecutor;
    }

    private def HeartBeat() = {
        callbackExecutor.Execute(() => {
            try {
                var orderId = -1;
                try {
                    orderId = GetNextOrderId;
                } catch {
                    case e: Throwable =>
                }


                if (orderId == -1) {
                    log.error("heartbeat failed , trying to reconnect");
                    clientSocket.eDisconnect();
                    Connect();
                    Resubscribe();
                }
            }
            catch {
                case ex: Throwable => log.error("Failed to heartbeat due to exception", ex);
            }
        });
    }

    val executor = Executors.newSingleThreadScheduledExecutor()

    def start() = {
        executor.scheduleAtFixedRate(new Runnable {
            override def run(): Unit = HeartBeat()
        }, 8, 8, TimeUnit.HOURS)
        Connect();
    }

    private def Connect() = {
        clientSocket.eConnect("127.0.0.1", port, clientId);
    }

    override def execDetails(reqId: Int, contract: Contract, execution: Execution) = {
        callbackExecutor.Execute(() => {

            super.execDetails(reqId, contract, execution);

            orders.find(_.IbId == execution.m_orderId) match {
                case None => log.error("execution, no order found for ib order id " + execution.m_orderId);
                case Some(entr) => tradeGateCallbacks.foreach(_.onTrade(new Trade(execution.m_shares, execution.m_price, entr.ClientOrder.OrderSide, entr.ClientOrder,
                   Instant.now() , entr.ClientOrder.Security)));

            }
        });
    }

    override def orderStatus(orderId: Int, status: String, filled: Int, remaining: Int, avgFillPrice: Double, permId: Int, parentId: Int,
                             lastFillPrice: Double, clientId: Int, whyHeld: String): Unit = {
        callbackExecutor.Execute(() => {
            log.info("OrderStatus. Id: " + orderId + ", Status: " + status + ", Filled" + filled + ", Remaining: " + remaining
              + ", AvgFillPrice: " + avgFillPrice + ", PermId: " + permId + ", ParentId: " + parentId + ", LastFillPrice: " + lastFillPrice + ", ClientId: " + clientId + ", WhyHeld: " + whyHeld + "\n");


            val entrOpt = orders.find(_.IbId == orderId);

            if (entrOpt.isEmpty) {
                log.error("no order found for ib id " + orderId);
                return;
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

                case "Inactive" => tradeGateCallbacks.foreach(_.onOrderStatus(entr.ClientOrder, OrderStatus.Rejected));

                case ("PreSubmitted" | "Submitted") => tradeGateCallbacks.foreach(_.onOrderStatus(entr.ClientOrder, OrderStatus.Accepted))

                case ("Cancelled" | "ApiCancelled" | "ApiCanceled") => tradeGateCallbacks.foreach(_.onOrderStatus(entr.ClientOrder, OrderStatus.Cancelled))

                case "Filled" => tradeGateCallbacks.foreach(_.onOrderStatus(entr.ClientOrder, OrderStatus.Done));

            }
        });
    }

    override def error(id: Int, errorCode: Int, errorMsg: String) = {
        callbackExecutor.Execute(() => {
            val find = subscriptions.find(_.RequestId == id)
            if (find.isDefined) {
                log.error("failed to subscribe %s message is %s".format(find.get.TickerId, errorMsg))
            }
        });
    }


    private def ConvertOrder(ord: Order): client.Order = {
        val order = new com.ib.client.Order() {
            m_action = if (ord.OrderSide == Side.Buy) "BUY" else "SELL"
            m_orderType = if (ord.OrdType == OrderType.Market) "MKT" else "LMT"
            m_totalQuantity = ord.Qty
        }
        if (ord.OrdType == OrderType.Market) {
            order.m_tif = "IOC";
        }
        return order;
    }

    private def Parse(ticker: String): Contract = {
        if (ticker2contract.contains(ticker)) {
            return ticker2contract(ticker);
        }

        assert(symbolMapping.contains(ticker),"no mapping for " + ticker)

        var contractSpec = symbolMapping(ticker);

        var contract = new Contract();

        for (v <- contractSpec.split(';')) {
            var mm = v.split('=');
            contract.getClass.getField(mm(0)).set(contract,mm(1))
        }
        ticker2contract(ticker) = contract;
        return contract;
    }

    private val subscriptions = new ArrayBuffer[Subscriptions]();
    private var clientId: Int =_


    override def tickSize(tickerId: Int, field: Int, size: Int) = {
        callbackExecutor.Execute(() => {
            var pr = subscriptions(tickerId).LastPriceQuote
            if (System.currentTimeMillis() - pr.DtGmt.toEpochMilli < 20) {
                pr.Vol = size;
                if (field == TickType.LAST_SIZE) {
                    subscriptions(tickerId).Listeners.foreach(_.apply(pr))
                    //log.info(string.Format("tick {0} sec {1} ", pr, subscriptions[tickerId].TickerId));
                }
            }
        });
    }

    override def tickPrice(tickerId: Int, field: Int, price: Double, canAutoExecute: Int) = {

        callbackExecutor.Execute(() => {
            if (price > 0) {
                subscriptions(tickerId).LastPriceQuote = new Tick(){
                    Last = price; dtGmt = Instant.now()
                }
            }else{
                log.error("wrong price for " + subscriptions(tickerId) + " price " + price);
            }


        });
    }

    private class Subscriptions (val TickerId: String, val contract: Contract) {
        val Listeners = new ArrayBuffer[Tick => Unit]()
        var LastPriceQuote: Tick = _
        var RequestId: Int = -1

        def resubscribe()={
            if(RequestId != -1) clientSocket.cancelMktData(RequestId);
            RequestId = nextSubscriptionReqId()
            clientSocket.reqMktData(RequestId, contract, "", false, new util.ArrayList[TagValue]());
        }
    }

    def nextSubscriptionReqId() : Int= {
        subscriptionTickerCounter += 1
        return subscriptionTickerCounter
    }

    private def Resubscribe() {
        subscriptions.foreach(_.resubscribe())
    }

    override def subscribeForTick(tickerId: String, lsn: (Tick) => Unit): Unit = {
        callbackExecutor.Execute(() => {
            var contract = Parse(tickerId);

            subscriptions.find(_.TickerId == tickerId) match {
                case Some(ss) => ss.Listeners += lsn;
                case None => {
                    val ss: Subscriptions = new Subscriptions(tickerId, contract){
                        Listeners += lsn
                    }
                    subscriptions +=  ss
                    ss.resubscribe()
                }
            }
        });

    }

    override def subscribeForOhlc(tickerId: String, lsn: (Ohlc) => Unit): Unit = {

    }
}
