package firelib.starts.qusma

import java.nio.file.{Path, Paths}
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}

import firelib.backtest.backtestStarter
import firelib.common._
import firelib.domain.Ohlc
import firelib.indicators.ATR
import firelib.utils.HeapQuantile
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class QusmaLowQuart extends BasketModel {
    var dayTimeSeries: Seq[ITimeSeries[Ohlc]] =_
    private var tradingHour = 0
    private var closeHour = 0
    private var minTimeseries: Seq[ITimeSeries[Ohlc]] =_
    private var capital = 10000
    private val entryPredicate = new mutable.HashMap[String, () => Boolean]()
    private val tickerNameToCloseHour = new mutable.HashMap[String, Int]()


    private val logger = LoggerFactory.getLogger(getClass)

    override protected def applyProperties(mprops: Map[String, String]) {
        dayTimeSeries = enableOhlcHistory(Interval.Day);
        minTimeseries = enableOhlcHistory(Interval.Min1);
        minTimeseries(0).listen(onMin);
        tradingHour = mprops("open.hour").toInt;


        capital = mprops("capital").toInt;

        /*
         * configuring tickets :
         *
         "SPY", 0.15 close hour = 13
         "DXJ", 0.23,  close hour = 11
         "QQQ", 0.22, RANGE/ATR5 < 0.8 close hour = 12
         "EEM", 0.18, ATR10 > 0.2 close hour = 9
         "AMLP", 0.2 close hour = 9

*/
        for (i <- 0 until dayTimeSeries.size) {

            if (stubs(i).security == "DXJ") {
                var index = i;
                entryPredicate("DXJ") = () => checkLowRangeCondition(index, 0.23);
                tickerNameToCloseHour("DXJ") = 11;
            }
            if (stubs(i).security == "SPY") {
                var index = i;
                entryPredicate("SPY") = () => checkLowRangeCondition(index, 0.15);
                tickerNameToCloseHour("SPY") = 13;
            }

            if (stubs(i).security == "AMLP") {
                var index = i;
                entryPredicate("AMLP") = () => checkLowRangeCondition(index, 0.2);
                tickerNameToCloseHour("AMLP") = 9;
            }

            //tickers with some filters

            if (stubs(i).security == "QQQ") {
                var index = i;
                var atr = new ATR(5, dayTimeSeries(index));
                val vola = () => calcRange(dayTimeSeries(index)) / atr.value;
                var quantile = new HeapQuantile(0.8, 1000);
                var dts = new mutable.HashSet[Instant]()
                entryPredicate("QQQ") = () => {
                    if (!dts.contains(dayTimeSeries(index)(0).dtGmtEnd)) {
                        quantile.addMetric(vola());
                        dts.add(dayTimeSeries(index)(0).dtGmtEnd);
                    }

                    log(s"quantile is ${quantile.value} vola is ${vola()} (must be quantile > vola )");

                    checkLowRangeCondition(index, 0.22) && quantile.value > vola();
                };
                tickerNameToCloseHour("QQQ") = 12;
            }

            if (stubs(i).security == "EEM") {
                val index = i;
                val atr = new ATR(10, dayTimeSeries(index));
                val quantile = new HeapQuantile(0.2, 1000);
                val dts = new mutable.HashSet[Instant]()
                entryPredicate("EEM") = () => {
                    if (!atr.value.isNaN && !dts.contains(dayTimeSeries(index)(0).dtGmtEnd)) {
                        quantile.addMetric(atr.value);
                        dts.add(dayTimeSeries(index)(0).dtGmtEnd);
                    }
                    log(s"quantile is ${quantile.value} atr is ${atr.value}")
                    checkLowRangeCondition(index, 0.18) && quantile.value < atr.value
                };
                tickerNameToCloseHour("EEM") = 9;
            }

        }

    }




    override def log(msg : String ) =
      {
          if(Instant.now().getEpochSecond - dtGmt.getEpochSecond < 24*3600*5)
            logger.info(msg);
      }


    private def checkLowRangeCondition(i: Int, threshold: Double): Boolean = {

        var dayTs = dayTimeSeries(i);
        var minTimeSerie = minTimeseries(i);

        if (dayTs.count < 2) {
            return false;
        }

        log("checking " + stubs(i).security);

        var range = calcRange(dayTs);

        log("range is  " + range);

        var metric = (dayTs(0).C - dayTs(0).L) / range;

        log("metric is  " + metric);

        if (metric < threshold && !minTimeSerie(0).interpolated) {
            log("check is true  as metric < threshold  " + threshold);
            return true;
        }
        log("check is false");
        return false;
    }

    private def calcRange(dayTs: ITimeSeries[Ohlc]): Double = {
        if (dayTs.count < 2) {
            return dayTs(0).range
        }
        log("last day " + dayTs(0));
        log("prev day " + dayTs(-1));
        var range = math.max(dayTs(-1).C, dayTs(0).H) - dayTs(0).L;
        return range;
    }

    private def onMin(ts: ITimeSeries[Ohlc]): Unit = {

        for (i <- 0 until minTimeseries.size) {
            if (dtGmt.isAfter(Instant.now().plus(-2, ChronoUnit.DAYS))) {
                log("on min : " + stubs(i).security + "  " + minTimeseries(i)(0));
            }
        }


        if (ts.count < 100) {
            return;
        }
        var nativeTime = dtGmt.atZone(ZoneId.of("America/New_York"));

        for (i <- 0 until minTimeseries.size) {
            if (nativeTime.getHour == tradingHour && nativeTime.getMinute >= 55) {
                if (position(i) == 0 && entryPredicate(stubs(i).security)() && !stubs(i).hasPendingState) {
                    var pos = (capital / minTimeseries(i)(0).C).toInt;
                    log("managing position for " + stubs(i).security + " to " + pos + " last price " + minTimeseries(i)(0).C);
                    managePosTo(pos, i);
                }
            }
        }

        for (i <- 0 until minTimeseries.size) {
            if (nativeTime.getHour == tickerNameToCloseHour(stubs(i).security) && !minTimeseries(i)(0).interpolated && position(i) != 0) {
                if (stubs(i).hasPendingState) {
                    log("Not flattening as security " + stubs(i).security + " has pending state ");
                } else {
                    log("Flatten all for " + stubs(i).security + " current position " + position(i));
                    stubs(i).flattenAll(None);
                }


            }
        }
    }

    override def onBacktestEnd(): Unit = {}



}



object QusmaLowQuart{
    def main(args : Array[String]) ={
        val config: ModelConfig = new ModelConfig()
        val configs: ArrayBuffer[TickerConfig] = config.tickerConfigs
        configs += new TickerConfig("DXJ","1MIN/STK/DXJ_1.csv",MarketDataType.Ohlc)
        configs += new TickerConfig("QQQ","1MIN/STK/QQQ_1.csv",MarketDataType.Ohlc)
        configs += new TickerConfig("EEM","1MIN/STK/EEM_1.csv",MarketDataType.Ohlc)
        configs += new TickerConfig("SPY","1MIN/STK/SPY_1.csv",MarketDataType.Ohlc)
        config.backtestStepInterval = Interval.Min1
        config.customParams("capital") = "5000"
        config.customParams("open.hour") = "15"

        val sampleRoot: Path = Paths.get("./strats/src/main/sampleRoot")
        config.reportRoot =  sampleRoot.resolve("reportRoot/qusma").toAbsolutePath.toString
        config.modelClassName = QusmaLowQuart.getClass.getName.replace("$","")
        config.backtestMode = BacktestMode.SimpleRun
        config.dataServerRoot = sampleRoot.resolve("dsRoot").toAbsolutePath.toString
        backtestStarter.runBacktest(config)

    }

}
