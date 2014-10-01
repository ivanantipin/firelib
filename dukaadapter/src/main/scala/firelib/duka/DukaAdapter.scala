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


    val orders = new ConcurrentHashMap[String, (IOrder, Order, Seq[IFillOrder])]()

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


    override def sendOrder(order: Order) = {
        if(!client.isConnected){
            executor.execute(() => tradeGateCallbacks.foreach(_.onOrderStatus(order, OrderStatus.Rejected)))
        }
        val task: Future[Option[AnyRef]] = context.executeTask(new Callable[Option[AnyRef]] {
            override def call(): Option[AnyRef] = {
                val dorder: IOrder = engine.submitOrder(order.id, Instrument.valueOf(order.security), orderCommand(order), order.qty.toDouble, 0, 20)
                orders(order.id) = (dorder, order, List[IFillOrder]())
                return None
            }
        })
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

    def toFireSide(cmd : IEngine.OrderCommand): Side ={
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
        Option(orders (orderId)) match {
            case Some(ord)  =>{
                cancelOrderInDuka(ord._1)
            }
            case None => log.error(s"trying to cancel non existed order, orderId=$orderId")
        }
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


    def cancelOrderInDuka(order : IOrder) {
        context.executeTask(new Callable[AnyRef] {
            override def call() : AnyRef = {
                engine.closeOrders(order)
                return null
            }
        })

    }



    def onTick(instrument: Instrument, iTick: ITick) = {
        if(log.isDebugEnabled){
            log.debug(s"tick received $iTick")
        }
        executor.execute(()=>{
            subs.find(_.TickerId == instrument.name()) match {
                case Some(s) =>{
                    val m =  iTick.getAsk + iTick.getBid
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

    def adjustAmt(amt : Double) : Int = (amt * 1000000).toInt

    def fireTrades(message: IMessage): Unit = {

        if(message.getReasons.contains(IMessage.Reason.ORDER_FULLY_FILLED) || message.getReasons.contains(IMessage.Reason.ORDER_CHANGED_AMOUNT)){

            val order: IOrder = message.getOrder

            orders.values().find(t=>t._1.getId == order.getId) match {

                case Some(tuple) =>{
                    val df: Seq[IFillOrder] = diff(tuple._3,order.getFillHistory)
                    executor.execute(()=>{
                        for(f <- df){
                            val trade: Trade = new Trade(adjustAmt(f.getAmount), f.getPrice,
                                toFireSide(order.getOrderCommand), tuple._2, Instant.now())
                            tradeGateCallbacks.foreach(_.onTrade(trade))
                        }

                        orders.replace(tuple._2.id,tuple.copy(_3 = (tuple._3 ++ df)))

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
        executor.execute(()=>{
            tradeGateCallbacks.foreach(tgc=>{
                orders.values().find(_._1.getId == dukaId) match {
                    case Some(t) => tgc.onOrderStatus(t._2,status)
                    case None => log.error(s"order not found for id ${dukaId}")
                }
            })
        })

    }

    def onAccount(iAccount: IAccount): Unit = {
        log.info(s"account received $iAccount")
    }

    def onStop {
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
