package firelib.backtest

import com.firelib.util.Utils
import firelib.domain.TickerConfig
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
        var updater = new BidAskUpdater(ret, tickerTypes);
        player.AddNextQuoteListener(updater);
        player.AddStepListener(updater);
        return ret;
    }


    protected def CreateModelBacktestEnvironment(cfg: ModelConfig, startDtGmt: DateTime, initializeWithDummyReaders: Boolean = false): (MarketDataPlayer, MarketDataDistributor) = {
        var intervalService = new IntervalService();

        var readers = if (initializeWithDummyReaders)
            new Array[ISimpleReader](0)
        else CreateReaders(cfg.TickerIds, startDtGmt, cfg.DataServerRoot);

        val mdPlayer = new MarketDataPlayer(readers);
        val ctx = new MarketDataDistributor(cfg.TickerIds, intervalService);
        mdPlayer.AddStepListener(intervalService);
        mdPlayer.AddQuoteListener(ctx);
        return (mdPlayer, ctx)
    }


    private def CreateReaders(tickerIds: Seq[TickerConfig], startDtGmt: DateTime, dsRoot: String): Array[ISimpleReader] = {
        val ret = new ArrayBuffer[ISimpleReader](tickerIds.length)

        for (t <- tickerIds) {
            var parser = //new UltraFastParser.UltraFastParser(Path.Combine(dsRoot, t.Path));
            if (!parser.Seek(startDtGmt)) {
                throw new Exception("failed to find start date " + startDtGmt);
            }
            parser.Read
            ret += parser
        }
        return ret.ToArray();
    }

    abstract def Run(cfg: ModelConfig)

    private def CalcStartDate(cfg: ModelConfig): DateTime = {
        var startDtGmt = if (cfg.StartDateGmt == null) DateTime. else Utils.ParseStandard(cfg.StartDateGmt);

        startDtGmt = cfg.interval.RoundTime(startDtGmt);

        var readers = CreateReaders(cfg.TickerIds, startDtGmt, cfg.DataServerRoot);

        if (readers.Max(r => r.PQuote -> DtGmt) > startDtGmt) {
            return readers.Max(r => r.PQuote -> DtGmt);
        }

        readers.foreach(r => r.Dispose)

        return startDtGmt;
    }


    protected def CalcTimeBounds(cfg: ModelConfig): (DateTime, DateTime) = {
        val startDt: DateTime = CalcStartDate(cfg)
        val endDt: DateTime = if (cfg.EndDate == null) DateTime.now() else Utils.ParseStandard(cfg.EndDate)
        return (startDt, endDt);
    }

    protected def WriteModelPnlStat(cfg: ModelConfig, model: IModel) = {
        new ReportWriter().Write(model, cfg, cfg.ReportRoot);
    }

    protected def InitModel(cfg: ModelConfig, mdPlayer: MarketDataPlayer, ctx: MarketDataDistributor, opts: Map[String, Int] = null): IModel = {
        var modelProps = cfg.CustomParams.toMap

        if (opts != null) {
            for (opt <- opts) {
                modelProps(opt._1) = "" + opt._2;
            }
        }
        return InitModelWithCustomProps(cfg, mdPlayer, ctx, modelProps);
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
        while (dtGmtcur.isBefore(endDtGmt)) {
            if (!mdPlayer.Step(dtGmtcur)) {
                break;
            }
            dtGmtcur = dtGmtcur.plusMillis(stepMs);
        }
    }
}
