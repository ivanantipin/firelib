package firelib.indicators

import java.time.Instant

import firelib.common.Side
import firelib.common.misc.PubTopic
import firelib.domain.Tick


class OrderInfo {
    var dt: Instant = Instant.EPOCH
    var side: Side = Side.None
    var qty: Int = 0
    var maxPrice: Double = Double.MinValue
    var minPrice: Double = Double.MaxValue
    var vwap: Double = Double.NaN
}

/*
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

 */

class OrderFromTicksProcessor (val subTopic : PubTopic[OrderInfo], classifyWithBidAsk  : Boolean = false){

    var lastTick: Tick = new Tick()

    private val classifySide: Tick => Side = if (classifyWithBidAsk) classifyByBidAsk else classifyByPrev

    private var currOrderInfo : OrderInfo = null

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
            if(currOrderInfo != null){
                currOrderInfo.vwap /= currOrderInfo.qty
                subTopic.publish(currOrderInfo)
            }
            currOrderInfo = new OrderInfo
            currOrderInfo.qty = tick.vol
            currOrderInfo.maxPrice = tick.last
            currOrderInfo.minPrice = tick.last
            currOrderInfo.vwap = tick.vol * tick.last;
            currOrderInfo.dt = tick.time
            currOrderInfo.side = cSide
        }
        lastTick = tick;
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

}
