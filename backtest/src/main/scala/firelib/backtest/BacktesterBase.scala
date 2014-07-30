package firelib.backtest

import java.time.Instant

import firelib.common._
import firelib.utils.{DateTimeExt, ReportWriter}

import scala.collection.mutable.ArrayBuffer

abstract class BacktesterBase(var marketStubFactory: String => IMarketStub = null) {
    private val defaultMarketStubFactory: String => IMarketStub = tickerId => new MarketStub(tickerId)

    marketStubFactory = if (marketStubFactory == null) defaultMarketStubFactory else marketStubFactory


    private def CreateMarketStubs(tickers: Seq[TickerConfig], player: MarketDataPlayer): Array[IMarketStub] = {
        var ret = new Array[IMarketStub](tickers.length);


        for (i <- 0 to tickers.length) {
            var stub = marketStubFactory(tickers(i).TickerId)
            ret(i) = stub
        }

        var updater = new BidAskUpdater(ret);
        player.AddListenerForAll(updater)
        player.AddStepListener(updater);
        return ret;
    }

    def appFuncOhlc(lsn : IMarketDataListener, idx : Int, curr : Ohlc, next : Ohlc) : Unit = lsn.onOhlc(idx,curr,next)

    def appFuncTick(lsn : IMarketDataListener, idx : Int, curr : Tick, next : Tick) : Unit = lsn.onTick(idx,curr,next)



    class TickerMdPlayerImpl[T <: Timed](val reader : ISimpleReader[T], val idx : Int, val func : (IMarketDataListener,Int, T, T)=>Unit ) extends TickerMdPlayer{

        val lsns = new ArrayBuffer[IMarketDataListener]()

        override def addListener(lsn: IMarketDataListener): Unit = {
            lsns += lsn
        }

        override def ReadUntil(chunkEndGmt:Instant): Boolean = {
            while (reader.CurrentQuote.DtGmt.isBefore(chunkEndGmt)) {
                val recordQuote = reader.CurrentQuote
                if (!reader.Read) {
                    return false;
                }
                lsns.foreach(ql=>func(ql,idx,recordQuote,reader.CurrentQuote));
            }
            return true
        }
        override def UpdateTimeZoneOffset(): Unit = reader.UpdateTimeZoneOffset()

        override def Dispose() = reader.Dispose()
    }



    def mapReadersToAware(readers : Seq[ISimpleReader[Timed]]) : Seq[TickerMdPlayer] ={
        return readers.zipWithIndex.map(t=>{
            if(t._1.CurrentQuote.isInstanceOf[Ohlc])
                new TickerMdPlayerImpl[Ohlc](t._1.asInstanceOf[ISimpleReader[Ohlc]], t._2, appFuncOhlc)
            else
                new TickerMdPlayerImpl[Tick](t._1.asInstanceOf[ISimpleReader[Tick]], t._2, appFuncTick)
        })
    }



    protected def CreateModelBacktestEnvironment(cfg: ModelConfig, startDtGmt:Instant, initializeWithDummyReaders: Boolean = false) : (MarketDataPlayer, MarketDataDistributor) = {
        val intervalService = new IntervalService();
        val readers : Seq[ISimpleReader[Timed]] = if (initializeWithDummyReaders)
            new Array[ISimpleReader[Timed]](0)
        else CreateReaders(cfg.tickerIds, startDtGmt, cfg.dataServerRoot);

        val mdPlayer = new MarketDataPlayer(mapReadersToAware(readers));

        val distributor = new MarketDataDistributor(readers.length,intervalService);
        mdPlayer.AddStepListener(intervalService);
        mdPlayer.AddListenerForAll(distributor)
        return (mdPlayer, distributor)
    }

    private def createOhlcReader(cfg : TickerConfig) : ISimpleReader[Ohlc] ={
        return null
    }

    private def createTickReader(cfg : TickerConfig) : ISimpleReader[Tick] ={
        return null
    }


    private def CreateReaders(tickerIds: Seq[TickerConfig], startDtGmt:Instant, dsRoot: String): Seq[ISimpleReader[Timed]] = {
        return tickerIds.map(t=>{
            val parser = if (t.mdType == MarketDataType.Tick) createTickReader(t) else createOhlcReader(t) //new UltraFastParser.UltraFastParser(Path.Combine(dsRoot, t.Path));
            assert(parser.Seek(startDtGmt), "failed to find start date " + startDtGmt)
            parser.Read
            parser.asInstanceOf[ISimpleReader[Timed]]
        })
    }

    def Run(cfg: ModelConfig)

    private def CalcStartDate(cfg: ModelConfig): Instant = {
        var startDtGmt = if (cfg.startDateGmt == null) Instant.EPOCH else DateTimeExt.ParseStandard(cfg.startDateGmt);

        startDtGmt = cfg.interval.roundTime(startDtGmt);

        var readers = CreateReaders(cfg.tickerIds, startDtGmt, cfg.dataServerRoot);

        val maxReadersStartDate = readers.maxBy(r =>r.CurrentQuote.DtGmt.getEpochSecond).CurrentQuote.DtGmt

        if (maxReadersStartDate.isAfter(startDtGmt)) {
            return maxReadersStartDate;
        }

        readers.foreach(_.Dispose)
        return startDtGmt;
    }


    protected def CalcTimeBounds(cfg: ModelConfig): (Instant,Instant) = {
        val startDt: Instant = CalcStartDate(cfg)
        val endDt: Instant = if (cfg.endDate == null) Instant now() else DateTimeExt.ParseStandard(cfg.endDate)
        return (startDt, endDt);
    }

    protected def WriteModelPnlStat(cfg: ModelConfig, model: IModel) = {
        ReportWriter.Write(model, cfg, cfg.reportRoot);
    }

    protected def InitModel(cfg: ModelConfig, mdPlayer: MarketDataPlayer, distributor: MarketDataDistributor, opts: Map[String, Int] = Map[String,Int]()): IModel = {
        return InitModelWithCustomProps(cfg, mdPlayer, distributor, cfg.customParams.toMap ++ opts.map(t => (t._1, "" + t._2)));
    }

    protected def InitModelWithCustomProps(cfg: ModelConfig, mdPlayer: MarketDataPlayer, ctx: MarketDataDistributor,
                                           modelProps: Map[String, String]): IModel = {
        val model = Class.forName(cfg.className).newInstance().asInstanceOf[IModel];
        var marketStubs = CreateMarketStubs(cfg.tickerIds, mdPlayer);
        model.initModel(modelProps, marketStubs, ctx);
        mdPlayer.AddStepListenerAtBeginning(model);
        return model;
    }


    protected def RunBacktest(dtGmt:Instant, endDtGmt:Instant, mdPlayer: MarketDataPlayer, stepMs: Int) {
        var dtGmtcur = dtGmt
        while (dtGmtcur.isBefore(endDtGmt) && mdPlayer.Step(dtGmtcur)) {
            dtGmtcur = dtGmtcur.plusMillis(stepMs);
        }
    }
}
