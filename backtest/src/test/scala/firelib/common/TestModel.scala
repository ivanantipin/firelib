package firelib.common

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util

import scala.collection.mutable.ArrayBuffer




class TestModel extends BasketModel with IMarketDataListener {

    var endTime = Instant.MIN

    val startTimesGmt = new ArrayBuffer[Instant]()

    private var hist: ITimeSeries[Ohlc] = _

    private val uniqTimes = new util.HashSet[Instant]();


    override def applyProperties(mprops: Map[String, String]) = {
        TestHelper.instance = this;

        hist = mdDistributor.activateOhlcTimeSeries(0, Interval.Min5, 10);
        hist.listen(On5Min)

        mdDistributor.addMdListener(this)
    }


    val bars = new ArrayBuffer[Ohlc]()

    private def On5Min(hh: ITimeSeries[Ohlc]): Unit = {
        bars += new Ohlc(hh(0))
    }


    var NumberOfTickes = 0;

    def onOhlc(idx: Int, ohlc: Ohlc, next: Ohlc) = ???

    def onTick(idx: Int, pQuote: Tick, next: Tick) = {
        NumberOfTickes += 1
        if (uniqTimes.contains(pQuote.DtGmt)) {
            throw new Exception("dupe time " + pQuote.DtGmt);
        }
        uniqTimes.add(pQuote.DtGmt)

        if (startTimesGmt.size == 0 || startTimesGmt.last.truncatedTo(ChronoUnit.DAYS) != pQuote.DtGmt.truncatedTo(ChronoUnit.DAYS)) {
            startTimesGmt += pQuote.DtGmt
        }
    }

    override def onBacktestEnd(): Unit = {}
}
