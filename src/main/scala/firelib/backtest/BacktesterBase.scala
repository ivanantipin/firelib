package firelib.backtest

import com.firelib.util.Utils
import firelib.domain._
import firelib.util.ReportWriter
import org.joda.time.DateTime

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

        override def ReadUntil(chunkEndGmt: DateTime): Boolean = {
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



    def mapReadersToAware(readers : Seq[ISimpleReader]) : Seq[TickerMdPlayer] ={
        return readers.zipWithIndex.map(t=>{
            if(t._1.CurrentQuote.isInstanceOf[Ohlc])
                new TickerMdPlayerImpl[Ohlc](t._1.asInstanceOf[ISimpleReader[Ohlc]], t._2, appFuncOhlc)
            else
                new TickerMdPlayerImpl[Tick](t._1.asInstanceOf[ISimpleReader[Tick]], t._2, appFuncTick)
        })
    }



    protected def CreateModelBacktestEnvironment(cfg: ModelConfig, startDtGmt: DateTime, initializeWithDummyReaders: Boolean = false): (MarketDataPlayer, MarketDataDistributor) = {
        var intervalService = new IntervalService();

        val readers : Seq[ISimpleReader] = if (initializeWithDummyReaders)
            new Array[ISimpleReader](0)
        else CreateReaders(cfg.TickerIds, startDtGmt, cfg.DataServerRoot);

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


    private def CreateReaders(tickerIds: Seq[TickerConfig], startDtGmt: DateTime, dsRoot: String): Seq[ISimpleReader] = {
        return tickerIds.map(t=>{
            val parser = if (t.mdType == MarketDataType.Tick) createTickReader(t) else createOhlcReader(t) //new UltraFastParser.UltraFastParser(Path.Combine(dsRoot, t.Path));
            if (!parser.Seek(startDtGmt)) {
                throw new Exception("failed to find start date " + startDtGmt);
            }
            parser.Read
            parser.asInstanceOf[ISimpleReader]
        })
    }

    abstract def Run(cfg: ModelConfig)

    private def CalcStartDate(cfg: ModelConfig): DateTime = {
        var startDtGmt = if (cfg.StartDateGmt == null) new DateTime(0) else Utils.ParseStandard(cfg.StartDateGmt);

        startDtGmt = cfg.interval.RoundTime(startDtGmt);

        var readers = CreateReaders(cfg.TickerIds, startDtGmt, cfg.DataServerRoot);

        val maxReadersStartDate = readers.maxBy(r =>r.CurrentQuote.DtGmt.getMillis).CurrentQuote.DtGmt

        if (maxReadersStartDate.isAfter(startDtGmt)) {
            return maxReadersStartDate;
        }

        readers.foreach(_.Dispose)
        return startDtGmt;
    }


    protected def CalcTimeBounds(cfg: ModelConfig): (DateTime, DateTime) = {
        val startDt: DateTime = CalcStartDate(cfg)
        val endDt: DateTime = if (cfg.EndDate == null) DateTime.now() else Utils.ParseStandard(cfg.EndDate)
        return (startDt, endDt);
    }

    protected def WriteModelPnlStat(cfg: ModelConfig, model: IModel) = {
        ReportWriter.Write(model, cfg, cfg.ReportRoot);
    }

    protected def InitModel(cfg: ModelConfig, mdPlayer: MarketDataPlayer, distributor: MarketDataDistributor, opts: Map[String, Int] = Map[String,Int]()): IModel = {
        return InitModelWithCustomProps(cfg, mdPlayer, distributor, cfg.CustomParams.toMap ++ opts.map(t => (t._1, "" + t._2)));
    }

    protected def InitModelWithCustomProps(cfg: ModelConfig, mdPlayer: MarketDataPlayer, ctx: MarketDataDistributor,
                                           modelProps: Map[String, String]): IModel = {
        val model = Class.forName(cfg.ClassName).newInstance().asInstanceOf[IModel];
        var marketStubs = CreateMarketStubs(cfg.TickerIds, mdPlayer);
        model.initModel(modelProps, marketStubs, ctx);
        mdPlayer.AddStepListenerAtBeginning(model);
        return model;
    }


    protected def RunBacktest(dtGmt: DateTime, endDtGmt: DateTime, mdPlayer: MarketDataPlayer, stepMs: Int) {
        var dtGmtcur = dtGmt
        while (dtGmtcur.isBefore(endDtGmt) && mdPlayer.Step(dtGmtcur)) {
            dtGmtcur = dtGmtcur.plusMillis(stepMs);
        }
    }
}
