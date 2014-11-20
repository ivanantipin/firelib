package firelib.common.core

import java.io.{FileOutputStream, OutputStreamWriter}
import java.nio.file.Paths
import java.time.Instant
import java.time.temporal.ChronoUnit

import firelib.common.config.ModelBacktestConfig
import firelib.common.interval.Interval
import firelib.common.misc.{dateUtils, utils}
import firelib.common.model.Model
import firelib.common.report.reportWriter
import firelib.common.timeseries.TimeSeries
import firelib.common.{Order, Trade, TradeGateCallbackAdapter}
import firelib.domain.Ohlc

class MdReportWriter (val ctx: SimpleRunCtx, val model : Model, val period : Int){

    val tss = new Array[TimeSeries[Ohlc]](model.orderManagers.size)

    var writeUntil : Instant = Instant.MIN

    var writers = model.orderManagers.map(om=>{
        val stream: FileOutputStream = new FileOutputStream(Paths.get(ctx.modelConfig.reportTargetPath,om.security).toFile)
        new OutputStreamWriter(stream)
    })

    writers.foreach(_.write("Date,O,H,L,C\n"))

    val lastWritten = Array.fill[Ohlc](model.orderManagers.size) ({new Ohlc{
        dtGmtEnd = Instant.MIN
    }})

    def write(ohlc : Ohlc, idx : Int) : Unit ={
        var lst = List[String](dateUtils.toStandardString(ohlc.dtGmtEnd))
        lst = lst :+ utils.dbl2Str(ohlc.O,6) :+ utils.dbl2Str(ohlc.H,6) :+ utils.dbl2Str(ohlc.L,6) :+ utils.dbl2Str(ohlc.C,6)
        writers(idx).write(lst.mkString(",") + "\n")
        lastWritten(idx) = ohlc
    }

    for(i <-0 until model.orderManagers.size){
        tss(i) = ctx.marketDataDistributor.activateOhlcTimeSeries(i,Interval.Min1,period)
        tss(i).listen(t=>{
            if(writeUntil.isAfter(tss(i)(0).DtGmt)){
                write(t.last,i)
            }
        })
        model.orderManagers(i).addCallback(new TradeGateCallbackAdapter(t=>onTrade(t,i),o=>onOrder(o,i)))
    }

    def onTrade(trade: Trade, i: Int): Unit = {
        writeOnEvent()

    }

    def writeOnEvent() {
        writeUntil = tss(0)(0).DtGmt.plus(period, ChronoUnit.MINUTES)
        for (i <- 0 until tss
          .length) {
            for (j <- -period to 0) {
                val ohlc: Ohlc = tss(i)(j)
                if (lastWritten(i).DtGmt.isBefore(ohlc.dtGmtEnd)) {
                    write(ohlc, i)
                }
            }
        }
    }

    def onOrder(order: Order, i: Int): Unit = {
        writeOnEvent()
    }


    def flash() : Unit = {
        writers.foreach(w=>{w.flush();w.close()})
    }

}

class BacktesterSimple  {


    def run(cfg: ModelBacktestConfig) : Unit = {

        reportWriter.clearReportDir(cfg.reportTargetPath)

        val ctx: SimpleRunCtx = new SimpleRunCtx(cfg)

        ctx.init()

        val model: Model = ctx.bindModelForParams(cfg.modelParams.toMap)

        val writer: MdReportWriter = new MdReportWriter(ctx,model,20)

        ctx.backtest.backtest()

        writer.flash()

        reportWriter.write(ctx.models(0), cfg, cfg.reportTargetPath)

    }

}
