package firelib.backtest

import breeze.stats.DescriptiveStats._
import com.firelib.util.Utils
import firelib.domain.Trade
import org.joda.time.Duration

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object BacktestStatisticsCalculator {
    def CalculateStatisticsForTrades(trades: Seq[Trade]): Map[StrategyMetric, Double] = {
        return CalculateStatisticsForCases(Utils.GetTradingCases(trades));
    }

    def CalculateStatisticsForCases(tradingCases: Seq[(Trade, Trade)]): Map[StrategyMetric, Double] = {
        var ret = mutable.HashMap[StrategyMetric, Double] {
            StrategyMetricEnum.Trades -> tradingCases.length
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

        val (avg, dis) = meanAndVariance(pnls)

        ret(StrategyMetricEnum.Sharpe) = avg / dis

        for (cas <- tradingCases) {
            if (cas._1.Qty != cas._2.Qty) {
                throw new Exception("trading case must have same volume");
            }
            if (cas._1.TradeSide == cas._2.TradeSide) {
                throw new Exception("trading case must have different sides");
            }
            val pn = Utils.PnlForCase(cas);
            pnl += pn;

            val ddelta = new Duration(cas._1.DtGmt, cas._2.DtGmt)
            holdingPeriodMins += ddelta.getStandardMinutes

            holdingPeriodMinsStat += ddelta.getStandardMinutes.toInt

            holdingPeriodSecs += ddelta.getStandardSeconds

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


        ret(StrategyMetricEnum.Pnl) = pnl;
        ret(StrategyMetricEnum.Pf) = math.min(-onlyProfit / onlyLoss, 4);
        ret(StrategyMetricEnum.MaxDdStat) = maxDrawDown;
        ret(StrategyMetricEnum.AvgPnl) = pnl / tradingCases.length;
        ret(StrategyMetricEnum.MaxProfit) = maxPnl;
        ret(StrategyMetricEnum.MaxLoss) = maxLoss;
        ret(StrategyMetricEnum.MaxLossesInRow) = maxLossesInRow;
        ret(StrategyMetricEnum.MaxProfitsInRow) = maxProfitsInRow;
        ret(StrategyMetricEnum.ProfitLosses) = (tradingCases.length - lossCount) / tradingCases.length.toDouble;
        ret(StrategyMetricEnum.AvgLoss) = (onlyLoss) / lossCount;
        ret(StrategyMetricEnum.AvgProfit) = onlyProfit / (tradingCases.length - lossCount);
        if (tradingCases.length != 0)
            ret(StrategyMetricEnum.AvgHoldingPeriodMin) = holdingPeriodMins / tradingCases.length;

        holdingPeriodMinsStat = holdingPeriodMinsStat.sorted;
        if (holdingPeriodMinsStat.length > 3) {
            ret(StrategyMetricEnum.MedianHoldingPeriodMin) = holdingPeriodMinsStat(holdingPeriodMinsStat.length / 2);
        }
        if (tradingCases.length != 0)
            ret(StrategyMetricEnum.AvgHoldingPeriodSec) = holdingPeriodSecs / tradingCases.length;
        return ret.toMap;
    }
}
