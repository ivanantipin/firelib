package firelib.duka

import java.time.Instant
import java.util
import java.util.concurrent.{Callable, ConcurrentHashMap, Future}

import com.dukascopy.api.IMessage.Type
import com.dukascopy.api.system.{ClientFactory, IClient, ISystemListener}
import com.dukascopy.api.{IAccount, IBar, IConsole, IContext, IEngine, IFillOrder, IHistory, IMessage, IOrder, IStrategy, ITick, Instrument, Period}
import firelib.common.threading.ThreadExecutor
import firelib.common.{Order, OrderStatus, OrderType, Side, Trade, TradeGateCallback}
import firelib.domain.{Ohlc, Tick}
import firelib.execution.{MarketDataProvider, TradeGate}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

class DukaAdapter extends TradeGate with MarketDataProvider with ISystemListener with IStrategy{

    var executor : ThreadExecutor =_


    var log: Logger = LoggerFactory.getLogger(getClass)

    var user : String=_

    var url : String=_

    var password : String =_


    case class OrderRecord(dukaOrder : Option[IOrder], order : Order, dukaTrades : Seq[IFillOrder])


    val orders = new ConcurrentHashMap[String, OrderRecord]()

    val doneOrders = new ConcurrentHashMap[String, OrderRecord]()

    val tradeGateCallbacks = new ArrayBuffer[TradeGateCallback]()

    var client: IClient =_

    var instruments: Array[Instrument] =_

    @volatile
    var lightReconnects = 3

    private var engine: IEngine = null
    private var history: IHistory = null
    private var console: IConsole = null
    private var context: IContext = null

    private val subs = new ArrayBuffer[Subscriptions]()

    private def round(amount : Double, decimalPlaces : Int, side : Side) : Double ={
        val method = if(side == Side.Sell) java.math.BigDecimal.ROUND_UP else java.math.BigDecimal.ROUND_DOWN
        new java.math.BigDecimal(amount).setScale(decimalPlaces, method).doubleValue();
    }


    override def sendOrder(order: Order) = {
        if(!client.isConnected){
            log.info(s"rejecting order $order as client not connected")
            executor.execute(() => tradeGateCallbacks.foreach(_.onOrderStatus(order, OrderStatus.Rejected)))
        }else{
            val tuple = new OrderRecord(None, order, List[IFillOrder]())
            val task: Future[Option[AnyRef]] = context.executeTask(new Callable[Option[AnyRef]] {
                override def call(): Option[AnyRef] = {
                    log.info(s"placing order to engine $order")
                    try {
                        val instrument: Instrument = Instrument.valueOf(order.security)
                        val roundedPrice: Double = round(order.price, instrument.getPipScale + 1, order.side)
                        val dorder: IOrder = engine.submitOrder(order.id, instrument, orderCommand(order), toDukaAmt(order.qty), roundedPrice, 20)
                        val copy = tuple.copy(dukaOrder = Option(dorder))
                        orders.put(order.id,copy)
                    }catch{
                            case e : Throwable =>{
                                executor.execute(()=>{
                                    tradeGateCallbacks.foreach(_.onOrderStatus(order,OrderStatus.Rejected))
                                })
                                log.error(s"failed placing order to engine $order", e)
                            }
                    }
                    return None
                }
            })
        }
    }

    def orderCommand (order : Order) : IEngine.OrderCommand ={
        (order.orderType, order.side) match {
            case (OrderType.Limit,Side.Buy) => IEngine.OrderCommand.BUYLIMIT
            case (OrderType.Limit,Side.Sell) => IEngine.OrderCommand.SELLLIMIT
            case (OrderType.Market,Side.Buy) => IEngine.OrderCommand.BUY
            case (OrderType.Market,Side.Sell) => IEngine.OrderCommand.SELL
            case (OrderType.Stop,Side.Buy) => IEngine.OrderCommand.BUYSTOP
            case (OrderType.Stop,Side.Sell) => IEngine.OrderCommand.SELLSTOP
        }
    }

    override def registerCallback(tgc: TradeGateCallback) = {
        tradeGateCallbacks += tgc
    }

    def toSide(cmd : IEngine.OrderCommand): Side ={
        if(cmd == IEngine.OrderCommand.SELL) Side.Sell else Side.Buy
    }



    override def configure(config: Map[String, String], callbackExecutor: ThreadExecutor) = {
        user = config("user")
        //"https://www.dukascopy.com/client/demo/jclient/jforex.jnlp"
        url = config("url")
        password = config("password")
        instruments = config("instruments").split(',').map(Instrument.valueOf(_))
        this.executor = callbackExecutor
    }

    override def cancelOrder(orderId: String) = {

        log.info(s"canelling order $orderId")

        Option(orders.getOrDefault(orderId,null)) match {
            case Some(ord)  =>{
                ord.dukaOrder match {
                    case Some(dukao) => try {
                        cancelOrderInDuka(dukao)
                    }catch {
                        case e : Throwable =>{
                            tradeGateCallbacks.foreach(_.onOrderStatus(ord.order,OrderStatus.CancelFailed))
                        }
                    }
                    case None => {
                        log.error(s"no duka order is inited trying to cancel : ${ord}")
                        tradeGateCallbacks.foreach(_.onOrderStatus(ord.order,OrderStatus.CancelFailed))
                    }
                }
            }
            case None => {
                log.error(s"trying to cancel non existed order, orderId=$orderId")
            }
        }
    }

    def cancelOrderInDuka(order : IOrder) {
        context.executeTask(new Callable[AnyRef] {
            override def call() : AnyRef = {
                order.setRequestedAmount(0)
                return null
            }
        }).get()
    }


    override def start()  = {
        client = ClientFactory.getDefaultInstance

        client.setSystemListener(this)

        log.info("Connecting...")

        client.connect(url, user, password)
    }

    private class Subscriptions(val TickerId: String) {
        val listeners = new ArrayBuffer[Tick => Unit]()
        var lastTick: Tick = _
        var requestId: Int = -1

        override def toString() : String ={
            s"tickerId=${TickerId}"
        }
    }




    override def onStart(p1: Long) = {}

    override def onConnect() = {
        log.info(s"setting subscribed instruments ${instruments.toList}")
        client.setSubscribedInstruments(new util.HashSet[Instrument](instruments.toList) )
        client.startStrategy(this)
        log.info("Connected")
        lightReconnects = 3
    }

    override def onDisconnect() = {
        log.warn("Disconnected")
        if (lightReconnects > 0) {
            client.reconnect
            lightReconnects -= 1
        }
        else {
            Thread.sleep(10000)
            client.connect(url, user, password)
        }
    }

    override def onStop(p1: Long) = {

    }

    def onStart(context: IContext) {
        this.engine = context.getEngine
        this.history = context.getHistory
        this.console = context.getConsole
        this.context = context
    }





    def onTick(instrument: Instrument, iTick: ITick) = {
        if(log.isDebugEnabled){
            log.debug(s"tick received $iTick")
        }
        executor.execute(()=>{
            subs.find(_.TickerId == instrument.name()) match {
                case Some(s) =>{
                    //FIXME set bid ask volumes
                    val vol = (iTick.getAskVolume + iTick.getBidVolume).toInt
                    val tick = new Tick()
                    tick.setBid(iTick.getBid)
                    tick.setAsk(iTick.getAsk)
                    tick.setDtGmt(Instant.now())
                    tick.setVol(vol)
                    s.listeners.foreach(_.apply(tick))
                }
                case None => {

                }
            }

        })
    }

    def onBar(instrument: Instrument, period: Period, iBar: IBar, iBar2: IBar) {
    }

    def diff (prevFills : Seq[IFillOrder],incomingFills : Seq[IFillOrder]): Seq[IFillOrder] ={
        val prevSorted: Seq[IFillOrder] = prevFills.sortBy(o=>o.getTime)
        val inSorted: Seq[IFillOrder] = incomingFills.sortBy(o=>o.getTime)
        inSorted.slice(prevSorted.length,inSorted.length)
    }

    def toDukaAmt(amt : Int) : Double = (amt.toDouble/OneMio)

    val OneMio: Int = 1000000

    def fromDukaAmt(amt : Double) : Int = (amt * OneMio).toInt

    def fireTrades(message: IMessage): Unit = {

        if(message.getReasons.contains(IMessage.Reason.ORDER_FULLY_FILLED) || message.getReasons.contains(IMessage.Reason.ORDER_CHANGED_AMOUNT)){

            val order: IOrder = message.getOrder

            orders.values().find(t=>t.dukaOrder.isDefined && t.dukaOrder.get.getId == order.getId) match {
                case Some(rec) =>{
                    val df: Seq[IFillOrder] = diff(rec.dukaTrades,order.getFillHistory)
                    executor.execute(()=>{

                        for(f <- df){

                            val trade: Trade = new Trade(fromDukaAmt(f.getAmount), f.getPrice,
                                toSide(order.getOrderCommand), rec.order, Instant.now())
                            tradeGateCallbacks.foreach(_.onTrade(trade))
                        }
                        val nrec: OrderRecord = rec.copy(dukaTrades = (rec.dukaTrades ++ df))
/* FIXME
                        if(rec.order.remainingQty.toDouble/rec.order.qty < 1/100000.0){
                            tradeGateCallbacks.foreach(_.onOrderStatus(rec.order,OrderStatus.Done))
                            orders.remove(rec.order.id)
                            doneOrders.put(rec.order.id, nrec)
                        }else{
                            orders.replace(rec.order.id,nrec)
                        }
*/

                    });

                }
                case None =>{
                    log.error(s"nothng found for message $message")
                }

            }

        }
    }

    def onMessage(message: IMessage) {

        log.info(s"message received $message")

        message.getType match {
            case Type.ORDER_SUBMIT_REJECTED => fire(message.getOrder.getId, OrderStatus.Rejected)
            case Type.ORDER_SUBMIT_OK => fire(message.getOrder.getId, OrderStatus.Accepted)
            case Type.ORDER_FILL_REJECTED => fire(message.getOrder.getId, OrderStatus.Cancelled)
            case Type.ORDER_CLOSE_REJECTED => fire(message.getOrder.getId, OrderStatus.CancelFailed)
            case Type.ORDER_CLOSE_OK => fire(message.getOrder.getId, OrderStatus.Cancelled)
            case Type.ORDER_FILL_OK | Type.ORDER_CHANGED_OK => fireTrades(message)
            case Type.MAIL =>
            case Type.NEWS =>
            case Type.CALENDAR =>
            case Type.NOTIFICATION =>
            case Type.INSTRUMENT_STATUS =>
            case Type.CONNECTION_STATUS =>
            case Type.STRATEGY_BROADCAST =>
            case Type.SENDING_ORDER =>
            case Type.STOP_LOSS_LEVEL_CHANGED =>
            case Type.WITHDRAWAL =>
        }
    }

    def fire(dukaId : String, status : OrderStatus) ={
        log.info(s"firing status $status for duka order id $dukaId")
        executor.execute(()=>{
            tradeGateCallbacks.foreach(tgc=>{
                orders.values().find(t=>t.dukaOrder.isDefined && t.dukaOrder.get.getId == dukaId) match {
                    case Some(rec) => tgc.onOrderStatus(rec.order,status)
                    case None => log.error(s"order not found for id ${dukaId} firing status $status")
                }
            })
        })

    }

    def onAccount(iAccount: IAccount): Unit = {
        log.info(s"account received $iAccount")
    }

    def onStop: Unit = {
        log.info(s"on stop called")
    }

    override def subscribeForTick(tickerId: String, lsn: (Tick) => Unit): Unit = {
        subs.find(_.TickerId == tickerId) match {
            case Some(s) =>{
                s.listeners += lsn
            }
            case None =>{
                subs += new Subscriptions(tickerId)
                subscribeForTick(tickerId,lsn)
            }
        }
    }

    override def subscribeForOhlc(tickerId: String, lsn: (Ohlc) => Unit): Unit = {}

}
