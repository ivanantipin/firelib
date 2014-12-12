package firelib.common.core

import java.io.{BufferedOutputStream, OutputStreamWriter}
import java.nio.file.StandardOpenOption._
import java.nio.file.{Files, Path, Paths}
import java.time.Duration

import firelib.common.OrderStatus
import firelib.common.config.ModelBacktestConfig
import firelib.common.interval.Interval
import firelib.common.misc.dateUtils._
import firelib.common.misc.{NonDurableTopic, WindowSlicer, utils}
import firelib.common.report.reportWriter
import firelib.domain.Ohlc


class MDWriter(val ctx : SimpleRunCtx, val path : Path){

    val colsDef = List[(String,Ohlc=>String)](
        ("DT",o=>o.dtGmtEnd.toStandardString),
        ("O",o=>utils.dbl2Str(o.O,5)),
        ("H",o=>utils.dbl2Str(o.H,5)),
        ("L",o=>utils.dbl2Str(o.L,5)),
        ("C",o=>utils.dbl2Str(o.C,5))
    )

    val eventTopic: NonDurableTopic[Any] = new NonDurableTopic[Any]

    val inTopic = new NonDurableTopic[Ohlc]()
    val outTopic = new NonDurableTopic[Ohlc]()

    ctx.marketDataDistributor.activateOhlcTimeSeries(0,Interval.Min1,3000).listen(ts=>inTopic.publish(ts(0)))

    val slicer = new WindowSlicer[Ohlc](outTopic, inTopic, eventTopic, Duration.ofMinutes(500))

    ctx.models(0).orderManagers(0).listenTrades(t=>eventTopic.publish(t))
    ctx.models(0).orderManagers(0).listenOrders(os=>{
        if(os.status == OrderStatus.New){
      //      eventTopic.publish(os)
        }
    })

    val stream =  new OutputStreamWriter(new BufferedOutputStream(Files.newOutputStream(path, CREATE, APPEND)))

    outTopic.subscribe(write(_))

    def write(ohlc : Ohlc): Unit = {

        stream.write(colsDef.map(_._2).map(_(ohlc)).mkString(";") :+ '\n')
        stream.flush()
    }
}


class BacktesterSimple  {


    def run(cfg: ModelBacktestConfig) : Unit = {

        reportWriter.clearReportDir(cfg.reportTargetPath)

        val ctx: SimpleRunCtx = new SimpleRunCtx(cfg)

        ctx.init()

        val output: ModelOutput = new ModelOutput(ctx.bindModelForParams(cfg.modelParams.toMap))

        new MDWriter(ctx,Paths.get(cfg.reportTargetPath).resolve("ohlcs.csv"))


        ctx.backtest.backtest()

        reportWriter.write(output, cfg, cfg.reportTargetPath)
    }

}
