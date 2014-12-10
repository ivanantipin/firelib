package firelib.common

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util

import firelib.common.interval.Interval
import firelib.common.misc.ohlcUtils
import firelib.common.model.BasketModel
import firelib.common.timeseries.TimeSeries
import firelib.domain.{Ohlc, Tick}

import scala.collection.mutable.ArrayBuffer




class TickTestModel extends BasketModel {

    var endTime = Instant.MIN

    val daysStarts = new ArrayBuffer[Instant]()

    private var hist: TimeSeries[Ohlc] = _

    val uniqTimes = new util.HashSet[Instant]()


    override def applyProperties(mprops: Map[String, String]) : Boolean = {
        testHelper.instanceTick = this

        hist = enableOhlcHistory(Interval.Min5, 10)(0)
        hist.listen(On5Min)

        bindComp.marketDataDistributor.listenTicks(0,onTick)
        true
    }


    val bars = new ArrayBuffer[Ohlc]()

    private def On5Min(hh: TimeSeries[Ohlc]): Unit = {
        bars += ohlcUtils.copy(hh(0))
        //System.out.println(hh(0))
    }


    var NumberOfTickes = 0

    def onOhlc(idx: Int, ohlc: Ohlc, next: Ohlc) = ???

    val ticks = new ArrayBuffer[Tick]()

    def onTick(tick: Tick) : Unit = {
        NumberOfTickes += 1
        assert(!uniqTimes.contains(tick.DtGmt),"dupe time " + tick.DtGmt)
        uniqTimes.add(tick.DtGmt)
        ticks += tick

        if (daysStarts.size == 0 || daysStarts.last.truncatedTo(ChronoUnit.DAYS) != tick.DtGmt.truncatedTo(ChronoUnit.DAYS)) {
            daysStarts += tick.DtGmt
        }
    }

    override def onBacktestEnd(): Unit = {}
}
