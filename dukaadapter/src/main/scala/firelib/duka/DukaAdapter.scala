package firelib.duka

import java.time.Instant
import java.util
import java.util.concurrent.{Callable, Future}

import com.dukascopy.api.IMessage.Type
import com.dukascopy.api.system.{ClientFactory, IClient, ISystemListener}
import com.dukascopy.api.{IAccount, IBar, IConsole, IContext, IEngine, IHistory, IMessage, IOrder, IStrategy, ITick, Instrument, Period}
import firelib.common.threading.ThreadExecutor
import firelib.common.{Order, OrderStatus, Side, TradeGateCallback}
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

    val orders = new ArrayBuffer[(IOrder, Order)]()

    val tradeGateCallbacks = new ArrayBuffer[TradeGateCallback]()

    var client: IClient =_

    var instruments: Array[Instrument] =_

    var lightReconnects = 3

    private var engine: IEngine = null
    private var history: IHistory = null
    private var console: IConsole = null
    private var context: IContext = null

    private val subs = new ArrayBuffer[Subscriptions]()


    override def sendOrder(order: Order) = {
        val task: Future[IOrder] = context.executeTask(new Callable[IOrder] {
            override def call(): IOrder = {
                val cmd = if (order.side == Side.Sell) IEngine.OrderCommand.SELL else IEngine.OrderCommand.BUY
                engine.submitOrder(order.id, Instrument.valueOf(order.security), cmd, order.qty.toDouble, 0, 20)
            }
        })
        orders += ((task.get,order))

    }

    override def registerCallback(tgc: TradeGateCallback) = {
        tradeGateCallbacks += tgc
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
        orders.find(_._2.id == orderId) match {
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
    }




    override def onStart(p1: Long) = {}

    override def onConnect() = {
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
        context.setSubscribedInstruments(new util.HashSet[Instrument](instruments.toList) )
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
                case None =>
            }

        })
    }

    def onBar(instrument: Instrument, period: Period, iBar: IBar, iBar2: IBar) {
    }

    def onMessage(message: IMessage) {

        message.getType match {
            case Type.ORDER_SUBMIT_REJECTED => fire(message.getOrder.getId, OrderStatus.Rejected)
            case Type.ORDER_SUBMIT_OK => fire(message.getOrder.getId, OrderStatus.Accepted)
            case Type.ORDER_FILL_REJECTED => fire(message.getOrder.getId, OrderStatus.Rejected)
            case Type.ORDER_CLOSE_REJECTED => fire(message.getOrder.getId, OrderStatus.CancelFailed)
            case Type.ORDER_CLOSE_OK => fire(message.getOrder.getId, OrderStatus.Cancelled)
            case Type.ORDER_FILL_OK => fire(message.getOrder.getId, OrderStatus.Done)
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

    def fire(orderId : String, status : OrderStatus) ={
        executor.execute(()=>{
            tradeGateCallbacks.foreach(tgc=>{
                orders.find(_._1.getId == orderId) match {
                    case Some(t) => tgc.onOrderStatus(t._2,status)
                    case None => log.error(s"order not found for id ${orderId}")
                }
            })
        })

    }

    def onAccount(iAccount: IAccount) {
    }

    def onStop {
    }

    override def subscribeForTick(tickerId: String, lsn: (Tick) => Unit): Unit = {}

    override def subscribeForOhlc(tickerId: String, lsn: (Ohlc) => Unit): Unit = {}

}
