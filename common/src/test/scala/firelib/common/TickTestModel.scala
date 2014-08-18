package firelib.common

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util

import firelib.domain.{Ohlc, Tick}

import scala.collection.mutable.ArrayBuffer




class TickTestModel extends BasketModel with IMarketDataListener {

    var endTime = Instant.MIN

    val daysStarts = new ArrayBuffer[Instant]()

    private var hist: ITimeSeries[Ohlc] = _

    val uniqTimes = new util.HashSet[Instant]()


    override def applyProperties(mprops: Map[String, String]) = {
        testHelper.instanceTick = this

        hist = mdDistributor.activateOhlcTimeSeries(0, Interval.Min5, 10)
        hist.listen(On5Min)

        mdDistributor.addMdListener(this)
    }


    val bars = new ArrayBuffer[Ohlc]()

    private def On5Min(hh: ITimeSeries[Ohlc]): Unit = {
        bars += new Ohlc(hh(0))
        //System.out.println(hh(0))
    }


    var NumberOfTickes = 0

    def onOhlc(idx: Int, ohlc: Ohlc, next: Ohlc) = ???

    val ticks = new ArrayBuffer[Tick]()

    def onTick(idx: Int, tick: Tick, next: Tick) = {
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
