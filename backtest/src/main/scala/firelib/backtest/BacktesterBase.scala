package firelib.backtest

import java.nio.file.{Path, Paths}
import java.time.Instant
import java.util.function.Supplier

import firelib.common._
import firelib.parser.{CommonIniSettings, IHandler, Parser, TokenGenerator}
import firelib.utils.DateTimeExt._
import firelib.utils.{DateTimeExt, ReportWriter}

import scala.collection.mutable.ArrayBuffer

abstract class BacktesterBase(var marketStubFactory: String => IMarketStub = null) {
    private val defaultMarketStubFactory: String => IMarketStub = tickerId => new MarketStub(tickerId)

    marketStubFactory = if (marketStubFactory == null) defaultMarketStubFactory else marketStubFactory


    private def createMarketStubs(tickers: Seq[TickerConfig], player: MarketDataPlayer): Array[IMarketStub] = {
        val ret = new Array[IMarketStub](tickers.length)

        for (i <- 0 until  tickers.length) {
            val stub = marketStubFactory(tickers(i).TickerId)
            ret(i) = stub
        }

        val updater = new BidAskUpdater(ret)
        player.addListenerForAll(updater)
        player.addStepListener(updater)
        return ret
    }

    def appFuncOhlc(lsn : IMarketDataListener, idx : Int, curr : Ohlc, next : Ohlc) : Unit = lsn.onOhlc(idx,curr,next)

    def appFuncTick(lsn : IMarketDataListener, idx : Int, curr : Tick, next : Tick) : Unit = lsn.onTick(idx,curr,next)



    class TickerMdPlayerImpl[T <: Timed](val reader : ISimpleReader[T],
                                         val idx : Int,
                                         val notifyListenerFunc : (IMarketDataListener,Int, T, T)=>Unit ) extends TickerMdPlayer{

        private val listeners = new ArrayBuffer[IMarketDataListener]()

        override def addListener(lsn: IMarketDataListener): Unit = listeners += lsn

        override def readUntil(chunkEndGmt:Instant): Boolean = {
            while (!reader.CurrentQuote.DtGmt.isAfter(chunkEndGmt)) {
                val recordQuote = reader.CurrentQuote
                if (!reader.Read) {
                    return false
                }
                listeners.foreach(notifyListenerFunc(_,idx,recordQuote,reader.CurrentQuote))
            }
            return true
        }

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



    protected def createModelBacktestEnv(cfg: ModelConfig, startDtGmt:Instant, initializeWithDummyReaders: Boolean = false) : (MarketDataPlayer, MarketDataDistributor) = {
        val intervalService = new IntervalService()
        val readers : Seq[ISimpleReader[Timed]] = if (initializeWithDummyReaders)
            new Array[ISimpleReader[Timed]](0)
        else CreateReaders(cfg.tickerIds, startDtGmt, cfg.dataServerRoot)

        val mdPlayer = new MarketDataPlayer(mapReadersToAware(readers))

        val distributor = new MarketDataDistributor(readers.length,intervalService)
        mdPlayer.addStepListener(intervalService)
        mdPlayer.addListenerForAll(distributor)
        return (mdPlayer, distributor)
    }

    private def createOhlcReader(cfg : TickerConfig, dsRoot : String) : ISimpleReader[Ohlc] ={
        val path: Path = Paths.get(dsRoot, cfg.Path)
        val iniFile: String = path.toAbsolutePath.getParent.resolve("common.ini").toAbsolutePath.toString
        val generator: TokenGenerator = new TokenGenerator(new CommonIniSettings().initFromFile(iniFile))
        return new Parser[Ohlc](path.toAbsolutePath.toString, generator.handlers.asInstanceOf[Array[IHandler[Ohlc]]], ohlcFactory)
    }


    val tickFactory = new Supplier[Tick]{
        override def get(): Tick = return new Tick()
    }

    val ohlcFactory = new Supplier[Ohlc]{
        override def get(): Ohlc = return new Ohlc()
    }


    private def createTickReader(cfg : TickerConfig, dsRoot : String) : ISimpleReader[Tick] ={
        val path: Path = Paths.get(dsRoot, cfg.Path)
        val iniFile: String = path.getParent.resolve("common.ini").toAbsolutePath.toString
        val generator: TokenGenerator = new TokenGenerator(new CommonIniSettings().initFromFile(iniFile))
        return new Parser[Tick](path.toAbsolutePath.toString, generator.handlers.asInstanceOf[Array[IHandler[Tick]]], tickFactory)
    }


    private def CreateReaders(tickerIds: Seq[TickerConfig], startDtGmt:Instant, dsRoot: String): Seq[ISimpleReader[Timed]] = {
        return tickerIds.map(t=>{
            val parser = if (t.mdType == MarketDataType.Tick) createTickReader(t, dsRoot) else createOhlcReader(t, dsRoot) //new UltraFastParser.UltraFastParser(Path.Combine(dsRoot, t.Path))
            assert(parser.seek(startDtGmt), "failed to find start date " + startDtGmt)
            parser.asInstanceOf[ISimpleReader[Timed]]
        })
    }

    def run(cfg: ModelConfig)

    private def calcStartDate(cfg: ModelConfig): Instant = {
        var startDtGmt = if (cfg.startDateGmt == null) Instant.EPOCH else cfg.startDateGmt.toDtGmt

        startDtGmt = cfg.interval.roundTime(startDtGmt)

        val readers = CreateReaders(cfg.tickerIds, startDtGmt, cfg.dataServerRoot)

        val maxReadersStartDate = readers.maxBy(r =>r.CurrentQuote.DtGmt.getEpochSecond).CurrentQuote.DtGmt

        readers.foreach(_.Dispose)

        return if (maxReadersStartDate.isAfter(startDtGmt)) maxReadersStartDate else startDtGmt
    }


    protected def CalcTimeBounds(cfg: ModelConfig): (Instant,Instant) = {
        val startDt: Instant = calcStartDate(cfg)
        val endDt: Instant = if (cfg.endDate == null) Instant now() else cfg.endDate.toDtGmt
        return (startDt, endDt)
    }

    protected def writeModelPnlStat(cfg: ModelConfig, model: IModel) = {
        ReportWriter.write(model, cfg, cfg.reportRoot)
    }

    protected def initModel(cfg: ModelConfig, mdPlayer: MarketDataPlayer, distributor: MarketDataDistributor, opts: Map[String, Int] = Map[String,Int]()): IModel = {
        return initModelWithCustomProps(cfg, mdPlayer, distributor, cfg.customParams.toMap ++ opts.map(t => (t._1, "" + t._2)))
    }

    protected def initModelWithCustomProps(cfg: ModelConfig, mdPlayer: MarketDataPlayer, distr: MarketDataDistributor,
                                           modelProps: Map[String, String]): IModel = {
        val model = Class.forName(cfg.className).newInstance().asInstanceOf[IModel]
        val marketStubs = createMarketStubs(cfg.tickerIds, mdPlayer)
        model.initModel(modelProps, marketStubs, distr)
        mdPlayer.addStepListenerAtBeginning(model)
        return model
    }


    protected def runBacktest(dtGmt:Instant, endDtGmt:Instant, mdPlayer: MarketDataPlayer, stepMs: Int) {
        var dtGmtcur = dtGmt
        while (dtGmtcur.isBefore(endDtGmt) && mdPlayer.step(dtGmtcur)) {
            dtGmtcur = dtGmtcur.plusMillis(stepMs)
        }
    }
}
