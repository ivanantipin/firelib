package firelib.spydia

import java.nio.file.{Path, Paths}
import java.time.{ZoneId, ZonedDateTime}

import firelib.backtest.{PositionCloserByTimeOut, backtestStarter}
import firelib.common._
import firelib.domain.Ohlc
import firelib.utils.HeapQuantile

import scala.collection.mutable.ArrayBuffer


class SpyDiaPair extends BasketModel{


    var tradingHour = 0

    var days: ArrayBuffer[ITimeSeries[Ohlc]] = _

    val highQuantile = new HeapQuantile(0.9,100)

    var money = 5000;

    var spyTs: ITimeSeries[Ohlc] = _

    var diaTs : ITimeSeries[Ohlc] = _

    var spyCloser: PositionCloserByTimeOut = _

    var diaCloser: PositionCloserByTimeOut = _

    override protected def applyProperties(mprops: Map[String, String]): Unit = {
        val days = enableOhlcHistory(Interval.Min60)
        spyTs = days(0)
        diaTs = days(1)
        days(0).listen(onHour)
        spyCloser  = new PositionCloserByTimeOut(stubs(0),24*3600)
        diaCloser = new PositionCloserByTimeOut(stubs(1),24*3600)
        tradingHour=mprops("trading.hour").toInt
    }

    def onHour(ts : ITimeSeries[Ohlc]) : Unit = {
        val localTime: ZonedDateTime = ts(0).dtGmtEnd.atZone(ZoneId.of("Europe/London"))

        if(!spyTs.last.interpolated){
            spyCloser.closePositionIfTimeOut(dtGmt)
        }
        if(!diaTs.last.interpolated){
            diaCloser.closePositionIfTimeOut(dtGmt)
        }

        if(!spyTs.last.interpolated  && localTime.getHour == tradingHour){
            val metric = (spyTs.last.C/diaTs.last.C)/(spyTs(-48).C/diaTs(-48).C)

            if(metric > highQuantile.value){
                managePosTo(-money/spyTs(0).C.toInt,0)
                managePosTo(money/diaTs(0).C.toInt,1)
            }

            if(!metric.isNaN){
                highQuantile.addMetric(metric)
            }
        }
    }

    override def onBacktestEnd(): Unit = {

    }
}

object SpyDiaPair{
    def main(args : Array[String]) ={
        val config: ModelConfig = new ModelConfig()
        val configs: ArrayBuffer[TickerConfig] = config.tickerConfigs
        configs += new TickerConfig("SPY","1MIN/STK/SPY_1.csv",MarketDataType.Ohlc)
        configs += new TickerConfig("DIA","1MIN/STK/DIA_1.csv",MarketDataType.Ohlc)
        config.backtestStepInterval = Interval.Min1
        config.optParams += new OptimizedParameter("trading.hour",0,24,1)
        val sampleRoot: Path = Paths.get("./strats/src/main/sampleRoot")
        config.reportRoot =  sampleRoot.resolve("reportRoot/spydia").toAbsolutePath.toString
        config.optimizedPeriodDays = 365
        config.optMinNumberOfTrades = 30
        config.modelClassName = SpyDiaPair.getClass.getName.replace("$","")
        config.backtestMode = BacktestMode.InOutSample
        config.dataServerRoot = sampleRoot.resolve("dsRoot").toAbsolutePath.toString
        backtestStarter.runBacktest(config)
    }

}
