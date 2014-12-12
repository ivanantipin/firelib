package firelib.indicators

import java.time.Instant
import java.time.temporal.ChronoUnit

import firelib.common.Side
import firelib.domain.Tick

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


class OrderInfo {
    var dt: Instant = _
    var side: Side = _
    var qty: Int = _
    var maxPrice: Double = Double.MinValue
    var minPrice: Double = Double.MaxValue
    var vwap: Double = _
}


trait OrderListener {
    def OnOrderEnqueue(orderInfo: OrderInfo)

    def OnOrderDequeue(orderInfo: OrderInfo)
}

class OrderFromTicksProcessor (orderFilterPredicate : (Int,Side)=>Boolean = (a,b)=>true , classifyWithBidAsk  : Boolean = false){

    //FIXME split components : order generator and window stats

    var lastTick: Tick = new Tick()

    val orders = new mutable.Queue[OrderInfo]()

    private val listeners = new ArrayBuffer[OrderListener]();

    private var classifySide : Tick=>Side = if (classifyWithBidAsk) classifyByBidAsk else classifyByPrev

    private var currOrderInfo = new OrderInfo

    def addTick(tick: Tick) {
        val cSide = classifySide(tick);

        if (tick.tickNumber == lastTick.tickNumber + 1 && tick.time.getEpochSecond == lastTick.time.getEpochSecond &&
          (cSide == Side.None || currOrderInfo.side == cSide)) {
            currOrderInfo.qty += tick.vol;
            currOrderInfo.vwap += tick.last * tick.vol;
            currOrderInfo.maxPrice = Math.max(currOrderInfo.maxPrice, tick.last);
            currOrderInfo.minPrice = Math.min(currOrderInfo.minPrice, tick.last);
        }
        else {
            if (orderFilterPredicate(currOrderInfo.qty, currOrderInfo.side)) {
                orders.enqueue(currOrderInfo);
                listeners.foreach(_.OnOrderEnqueue(currOrderInfo))
                currOrderInfo = new OrderInfo
            }
            currOrderInfo.qty = tick.vol
            currOrderInfo.maxPrice = tick.last
            currOrderInfo.minPrice = tick.last
            currOrderInfo.vwap = tick.vol * tick.last;
            currOrderInfo.dt = tick.time
            currOrderInfo.side = cSide
        }

        trimWindow()

        lastTick = tick;
    }

    class TimeWindowTrimmer(val windowSeconds: Int) extends (()=>Unit)  {

        override def apply(): Unit = {
            var limitDt = lastTick.time.plus(-windowSeconds, ChronoUnit.SECONDS);
            while (orders.length > 0 && orders.last.dt.isBefore(limitDt)) {
                var d = orders.dequeue()
                onOrderDequeue(d);
            }
        }
    }

    class VolumeWindowTrimmer(val volumeWindow: Int) extends (()=>Unit) with OrderListener  {

        var bidsVolume: Int = _
        var asksVolume: Int = _


        override def apply(): Unit = {
            while (orders.length != 0 && (bidsVolume + asksVolume) > volumeWindow) {
                var oinfo = orders.dequeue();
                if(oinfo.side == Side.Sell){
                    bidsVolume+=oinfo.qty
                }else{
                    asksVolume+=oinfo.qty
                }
                onOrderDequeue(oinfo);
            }
        }

        override def OnOrderEnqueue(orderInfo: OrderInfo): Unit = {
            if(orderInfo.side == Side.Sell){
                bidsVolume+=orderInfo.qty
            }else{
                asksVolume+=orderInfo.qty
            }
        }

        override def OnOrderDequeue(orderInfo: OrderInfo): Unit = {}
    }


    var trimWindow : ()=>Unit =_

    def enableVolumeTrimmer(volume : Int) : OrderFromTicksProcessor = {
        trimWindow=new this.VolumeWindowTrimmer(volume)
        this
    }

    def enableTimeTrimmer(secs : Int) : OrderFromTicksProcessor = {
        trimWindow=new this.TimeWindowTrimmer(secs)
        this
    }


    def addOrderListener(listener: OrderListener) {
        listeners += listener
    }

    private def classifyByPrev(tick: Tick): Side = {
        return if(compareDbls(tick.last, lastTick.last) > 0) Side.Buy else Side.Sell
    }

    private def classifyByBidAsk(tick: Tick): Side = {
        var c = compareDbls(tick.last, tick.bid);
        if (c == 0) {
            return Side.Sell;
        }
        c = compareDbls(tick.last, tick.ask);
        if (c == 0) {
            return Side.Buy;
        }
        return Side.None;
    }

    private def compareDbls(a: Double, b: Double): Int = {
        val ep = 0.00000001;
        var d = a - b;
        if (d > ep) {
            return 1;
        }
        if (d < -ep) {
            return -1;
        }
        return 0;
    }

    def onOrderDequeue(orderInfo: OrderInfo) = listeners.foreach(_.OnOrderDequeue(orderInfo))

}
