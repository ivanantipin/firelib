package firelib.backtest

import java.time.Duration

import breeze.stats.DescriptiveStatsTrait
import firelib.common._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object BacktestStatisticsCalculator  extends DescriptiveStatsTrait {
    def CalculateStatisticsForTrades(trades: Seq[Trade]): Map[StrategyMetric, Double] = {
        return CalculateStatisticsForCases(Utils.GetTradingCases(trades));
    }

    def CalculateStatisticsForCases(tradingCases: Seq[(Trade, Trade)]): Map[StrategyMetric, Double] = {
        var ret = mutable.HashMap[StrategyMetric, Double] {
            StrategyMetric.Trades -> tradingCases.length
        }


        var maxPnl = 0.0;
        var maxLoss = 0.0;
        var pnl = 0.0;
        var onlyLoss = 0.0;
        var onlyProfit = 0.0;
        var maxProfitsInRow = 0;
        var maxLossesInRow = 0;
        var lossCount = 0;
        var currProfitsInRow = 0;
        var currLossesInRow = 0;
        var maxDrawDown = 0.0;
        var maxReachedPnl = 0.0;

        var holdingPeriodMins = 0;
        var holdingPeriodMinsStat = new ArrayBuffer[Int]();
        var holdingPeriodSecs = 0;

        var pnls = tradingCases.map(Utils.PnlForCase)

        //FIXME  val avg = mean(pnls)
        //FIXME val dis  = variance(pnls)

        //FIXME ret(StrategyMetric.Sharpe) = avg / dis

        for (cas <- tradingCases) {
            if (cas._1.Qty != cas._2.Qty) {
                throw new Exception("trading case must have same volume");
            }
            if (cas._1.TradeSide == cas._2.TradeSide) {
                throw new Exception("trading case must have different sides");
            }
            val pn = Utils.PnlForCase(cas);
            pnl += pn;

            val ddelta = Duration.between(cas._1.DtGmt, cas._2.DtGmt)
            holdingPeriodMins += ddelta.toMinutes.toInt

            holdingPeriodMinsStat += ddelta.toMinutes.toInt

            holdingPeriodSecs += ddelta.getSeconds.toInt

            if (pnl > maxReachedPnl) {
                maxReachedPnl = pnl;
            }
            if (maxDrawDown < maxReachedPnl - pnl) {
                maxDrawDown = maxReachedPnl - pnl;
            }
            maxPnl = math.max(pn, maxPnl);
            maxLoss = math.min(pn, maxLoss);
            if (pn < 0) {
                lossCount += 1;
                onlyLoss += pn;
                currProfitsInRow = 0;
                currLossesInRow += 1;
                maxLossesInRow = math.max(maxLossesInRow, currLossesInRow);
            }
            else {
                onlyProfit += pn;
                currLossesInRow = 0;
                currProfitsInRow += 1;
                maxProfitsInRow = math.max(maxProfitsInRow, currProfitsInRow);
            }
        }


        ret(StrategyMetric.Pnl) = pnl;
        ret(StrategyMetric.Pf) = math.min(-onlyProfit / onlyLoss, 4);
        ret(StrategyMetric.MaxDdStat) = maxDrawDown;
        ret(StrategyMetric.AvgPnl) = pnl / tradingCases.length;
        ret(StrategyMetric.MaxProfit) = maxPnl;
        ret(StrategyMetric.MaxLoss) = maxLoss;
        ret(StrategyMetric.MaxLossesInRow) = maxLossesInRow;
        ret(StrategyMetric.MaxProfitsInRow) = maxProfitsInRow;
        ret(StrategyMetric.ProfitLosses) = (tradingCases.length - lossCount) / tradingCases.length.toDouble;
        ret(StrategyMetric.AvgLoss) = (onlyLoss) / lossCount;
        ret(StrategyMetric.AvgProfit) = onlyProfit / (tradingCases.length - lossCount);
        if (tradingCases.length != 0)
            ret(StrategyMetric.AvgHoldingPeriodMin) = holdingPeriodMins / tradingCases.length;

        holdingPeriodMinsStat = holdingPeriodMinsStat.sorted;
        if (holdingPeriodMinsStat.length > 3) {
            ret(StrategyMetric.MedianHoldingPeriodMin) = holdingPeriodMinsStat(holdingPeriodMinsStat.length / 2);
        }
        if (tradingCases.length != 0)
            ret(StrategyMetric.AvgHoldingPeriodSec) = holdingPeriodSecs / tradingCases.length;
        return ret.toMap;
    }
}
