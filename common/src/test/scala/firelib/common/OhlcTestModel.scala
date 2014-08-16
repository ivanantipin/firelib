package firelib.common

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util

import firelib.domain.Ohlc

import scala.collection.mutable.ArrayBuffer

class OhlcTestModel extends BasketModel {

    var endTime = Instant.MIN


    val startTimesGmt = new ArrayBuffer[Instant]();


    private var hist: ITimeSeries[Ohlc] = _


    var dayHist: ITimeSeries[Ohlc] = _


    val uniqTimes = new util.HashSet[Instant]()


    val bars = new ArrayBuffer[Ohlc]()


    override def applyProperties(mprops: Map[String, String]) = {
        testHelper.instanceOhlc = this
        hist = enableOhlcHistory(Interval.Min5, 10)(0);
        hist.listen(On5Min)
        dayHist = enableOhlcHistory(Interval.Day, 10)(0);

    }


    def On5Min(hh: ITimeSeries[Ohlc]): Unit = {
        if (dayHist.count > 0 && dayHist(0).dtGmtEnd.truncatedTo(ChronoUnit.DAYS) != dayHist(0).dtGmtEnd) {
            throw new Exception("time of day ts not correct");
        }

        if (dtGmt != hh(0).getDtGmtEnd) {
            throw new Exception("time is not equal");
        }
        bars += new Ohlc(hh(0));

        if (bars.size > 1) {
            if ((hh(0).getDtGmtEnd.toEpochMilli - hh(-1).getDtGmtEnd.toEpochMilli) != 5*60*1000) {
                throw new Exception("not 5 min diff " + hh(0).getDtGmtEnd + " -- " + hh(-1).getDtGmtEnd);
            }
        }
        AddOhlc(new Ohlc(hh(0)));
    }

    def AddOhlc(pQuote: Ohlc) = {
        if (uniqTimes.contains(pQuote.dtGmtEnd)) {
            throw new Exception("dupe time " + pQuote.dtGmtEnd);
        }
        uniqTimes.add(pQuote.getDtGmtEnd)

        if (startTimesGmt.size == 0 || startTimesGmt.last.truncatedTo(ChronoUnit.DAYS) != pQuote.DtGmt.truncatedTo(ChronoUnit.DAYS)) {
            startTimesGmt += pQuote.DtGmt
        }
    }

    override def onBacktestEnd(): Unit = {}
}
