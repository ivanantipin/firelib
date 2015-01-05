package com.firelib.plaza

import java.time.Instant
import java.util.concurrent.Executors

import firelib.common.misc.{NonDurableChannel, SubChannel, utils}
import firelib.common.threading.{ThreadExecutor, ThreadExecutorImpl}
import firelib.common.tradegate.TradeGate
import firelib.common.{Order, OrderType, Side, Trade}
import firelib.domain.OrderState
import firelib.execution.Configurable
import ru.micexrts.cgate._
import ru.micexrts.cgate.messages._

import scala.collection.mutable.ArrayBuffer
import scala.util.Random
;


case class ErrorCode(code : Int, name : String){
    val isOk = code == 0
}

object ErrorCode{

    val codes = List(
        ErrorCode(131072,"RANGE_BEGIN"),
        ErrorCode(0,"OK"),
        ErrorCode(131072,"INTERNAL"),
        ErrorCode(131073,"INVALID_ARGUMENT"),
        ErrorCode(131074,"UNSUPPORTED"),
        ErrorCode(131075,"TIMEOUT"),
        ErrorCode(131076,"MORE"),
        ErrorCode(131077,"INCORRECT_STATE"),
        ErrorCode(131078,"DUPLICATE_ID"),
        ErrorCode(131079,"BUFFER_TOO_SMALL"),
        ErrorCode(131080,"OVERFLOW"),
        ErrorCode(131081,"UNDERFLOW")
    )

    def apply(code : Int) : Option[ErrorCode]=codes.find(_.code == code)
}


object StateCode{
    val closed=StateCode(0,"CLOSED")
    val error=StateCode(1,"ERROR")
    val opening=StateCode(2,"OPENING")
    val active=StateCode(3,"ACTIVE")

    val codes = List(closed,error,opening,active)

    def apply(code : Int) : Option[StateCode]=codes.find(_.code == code)
}

case class StateCode(code : Int, name : String)



case class ListenerType(ltype : String)

object ListenerType{
    val p2repl = new ListenerType("p2repl")
    val p2mqreply = new ListenerType("p2mqreply")
    val p2sys = new ListenerType("p2sys")
}





case class CommandResponse(id : Int) extends AnyVal{


}





object CommandResponse {
    val futAddOrder = new CommandResponse(101)
}



case class OrderRec(order : Order, userId : Int, plazaOrderId : Option[Int]){
    val tradeTopic = new NonDurableChannel[Trade]
    val orderStateTopic = new NonDurableChannel[OrderState]


}


class OrderRegistry{
    def updatePlazaOrderId(userId: Int, plazaOrderId: String): Unit = {

    }

    def getByOrderId(ordId: String): Option[OrderRec] = {
        None
    }


    val list = new ArrayBuffer[OrderRec]()

    def add(rec: OrderRec): Unit = {
        list += rec
    }

    def getByUserId(userId: Int) : Option[OrderRec] = {
        None
    }


}




case class StateChange(previous : StateCode, current : StateCode)

class ConnectionWrapper(val connection: Connection, processTimeMs : Int) extends Runnable {

    @volatile
    var onStateChange : ConnectionWrapper=>Unit = _

    @volatile
    var onProcessEnd : ConnectionWrapper=>Unit = _

    @volatile
    var currentState = StateCode(connection.getState).get


    override def run(): Unit = {
        try {
            StateCode(connection.getState()) match {
                case Some(st) => {
                    if (st != currentState && onStateChange != null) {
                        currentState = st
                        onStateChange(this)

                    }
                    st match {
                        case StateCode.error => connection.close()
                        case StateCode.closed => connection.open("");
                        case StateCode.active =>
                            val code: Int = connection.process(processTimeMs)
                            ErrorCode(code) match {
                                case Some(c) if !c.isOk => {
                                    println(s"connection process is not OK $c");
                                }
                                case None => println(s"unknown code $code");
                                case _ => //OK

                            }
                    }
                }
                case None =>
            }
        }finally {
            if(onProcessEnd != null){
                onProcessEnd(this)
            }

        }

    }
}



class FieldsExtractor(val seq : Seq[(String,Value=>Any)]) extends (DataMessage=>RecordCopy){
    override def apply(msg: DataMessage): RecordCopy = {
        new RecordCopy(seq.map({case (name,func) => (name,func(msg.getField(name))) }))
    }
}

class RecordCopy(lst : Seq[(String,Any)]){
    private val map: Map[String, Any] = lst.toMap
    def apply[T](fldName : String) : T =  map(fldName).asInstanceOf[T]
}




class PlazaTradeGate extends TradeGate with Configurable{

    private var connection : Connection = _
    private var publisher : Publisher = _
    private var listener : Listener = _

    final val RouterLogin_msgid = 1;
    final val RouterLogout_msgid = 2;

    final val RouterConnected_msgid = 1;
    final val RouterDisconnected_msgid = 2;
    final val ConnectionConnected_msgid = 3;
    final val ConnectionDisconnected_msgid = 4;
    final val LogonFailed_msgid = 5;


    def getOrderType(order : Order): Unit ={
        order.orderType match {
            case OrderType.Limit =>0
            case OrderType.Market =>0
            case OrderType.Stop =>0
        }

    }

    def orderSide(order : Order): Int ={
        order.orderType match {
            case Side.Buy =>1
            case Side.Sell =>2
        }
    }

    val registry = new OrderRegistry




    def nextUserId() : Int = {
        new Random().nextInt()
    }



    /**
         * just order send
         */
    override def sendOrder(order: Order): (SubChannel[Trade], SubChannel[OrderState]) = {

        val cr : Publisher=>DataMessage = p=> {
            val message = publisher.newMessage (MessageKeyType.KEY_NAME, "FutAddOrder").asInstanceOf[DataMessage]
            message.getField ("broker_code").set ("HB00")
            message.getField ("client_code").set ("000")
            message.getField ("isin").set ("RTS-6.12")
            message.getField ("dir").set (orderSide(order))
            message.getField ("type").set (1)
            message.getField ("amount").set (order.qty)
            message.getField ("price").set (utils.dbl2Str (order.price, 2) )
            message.getField ("ext_id").set (0)
            message.setUserId(nextUserId)
            message
        }

        val rec: OrderRec = new OrderRec(order, nextUserId(), None)
        registry.add(rec)
        process(publisher,cr,(msg=>publisher.post (msg, PublishFlag.NEED_REPLY)))
        (rec.tradeTopic,rec.orderStateTopic)
    }

    /**
     * order cancel
     */
    override def cancelOrder(order: Order): Unit = {

        val rec : Option[OrderRec] = registry.getByOrderId(order.id)

        rec  match {
            case Some(rec) =>{
                val cr : Publisher=>DataMessage = p=> {
                    val message = publisher.newMessage (MessageKeyType.KEY_NAME, "FutDelOrder").asInstanceOf[DataMessage]
                    message.getField ("order_id").set ("HB00")
                    message
                }
                process(publisher,cr,(msg=>publisher.post (msg, PublishFlag.NEED_REPLY)))
            }
            case None => //log error
        }

    }

    var listenerOfDeals : Listener = _

    val executor = Executors.newFixedThreadPool(11)

    /**
    TYPE
    p2repl
    p2mqreply
    p2ordbook
    p2sys
    */

    class ListnerWrapper(val conn : ConnectionWrapper, val refName : String, val listProt : ListenerType){
    }



    override def start(config: Map[String, String], callbackExecutor: ThreadExecutor): Unit = {
        CGate.open("ini=jsend_mt.ini;key=11111111");

        val cc: ConnectionWrapper = new ConnectionWrapper(new Connection("p2tcp://127.0.0.1:4001;app_name=jtest_send"),0)

        cc.onProcessEnd = executor.submit(_)

        cc.onStateChange = (st=>{
            if(st.currentState == StateCode.active){
                publisher = new Publisher(connection, "p2mq://FORTS_SRV;category=FORTS_MSG;timeout=5000;scheme=|FILE|forts_messages.ini|message;name=cmd");
                listener = new Listener(connection, "p2mqreply://;ref=cmd", new ResponseListener());
                listenerOfDeals = new Listener(connection, "p2repl://FORTS_FUTTRADE_REPL", new ResponseListener());
                publisher.open("");
                listener.open("");
            }
        })
        executor.submit(cc)
    }



    def process(pub : Publisher, create : Publisher=>DataMessage, process : DataMessage=>Unit): Unit = {
        try {
            val msg: DataMessage = create(pub)
            try {
                process(msg)
            }finally {
                msg.dispose()
            }
        }catch {
            case e : Exception =>
        }
    }

    def putUnmatchedTrade(orderId : String, trade : Trade): Unit ={

    }

    val dummyOrder = new Order(OrderType.Limit,1,1,Side.Sell,"SS","",Instant.now())

    val fx = new FieldsExtractor(List[(String,Value=>Any)](
        ("amount",v=>v.asInt ),
        ("price",v=>v.asString ),
        ("order_id",v=>v.asString )
    ))


    class TradesListener extends ISubscriber{
        override def onMessage(p1: Connection, p2: Listener, message : Message): Int = {
            message.getType() match
            {

                case MessageType.MSG_DATA =>
                    val dataMsg = message.asInstanceOf[DataMessage];

                    val recc : RecordCopy = fx(dataMsg)

                    registry.getByOrderId(recc("order_id")) match {
/*
                        case Some(rec) =>{
                            rec.tradeTopic.publish(new Trade(recc[Int]("amount"),price,rec.order,Instant.now()))
                        }
                        case None =>putUnmatchedTrade(orderId,new Trade(qty,price,dummyOrder,Instant.now()))
*/
                    }
            }
            0

        }
    }


    class ResponseListener extends ISubscriber {

        @Override
        def onMessage(conn: Connection, listener: Listener, message: Message): Int = {
            message.getType() match
            {
                case MessageType.MSG_DATA =>
                    val dataMsg = message.asInstanceOf[DataMessage];
                    CommandResponse(dataMsg.getType) match {
                        case CommandResponse.futAddOrder =>{
                            registry.getByUserId(dataMsg.getUserId) match {
                                case Some(ss)=>{
                                    if(dataMsg.getField("code").asInt() == ErrorCode.OK){
                                        val orderId  = dataMsg.getField("order_id").asString()
                                        registry.updatePlazaOrderId(dataMsg.getUserId,orderId)
                                    }
                                }
                            }
                        }
                    }
                case MessageType.MSG_P2MQ_TIMEOUT=>
                    println("Timeout");
                case MessageType.MSG_OPEN =>
                    println("Msg open")
                case MessageType.MSG_CLOSE =>
                    println("Msg close");
                case _ =>
                    println(message.toString());
            }


            return 0;
        }
    }

}


object Run{
    def main(args: Array[String]) {
        val gate: PlazaTradeGate = new PlazaTradeGate
        gate.start(Map(), new ThreadExecutorImpl())
    }
}
