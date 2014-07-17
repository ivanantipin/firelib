package firelib.backtest

import com.firelib.util.Utils
import firelib.domain.{ExecutionEstimates, Trade}

import scala.collection.Map
import scala.collection.mutable.ArrayBuffer

trait IReportProcessor {
    def Estimates: ArrayBuffer[ExecutionEstimates]

    def BestModelsList: ArrayBuffer[IModel]
}


class ReportProcessor(val estimatedMetrics: (Seq[(Trade, Trade)]) => Map[StrategyMetric, Double], val optimizedFunctionName: StrategyMetric,

                      val optParams: Seq[String], val topModelsToKeep: Int = 3, val minNumberOfTrades: Int = 1, val removeOutlierTrades: Int = 2) extends IReportProcessor {


    private val bestModels = new ArrayBuffer[(Double, IModel, Map[StrategyMetric, Double])]();
    var Estimates = new ArrayBuffer[ExecutionEstimates]()

    def BestModels: ArrayBuffer[IModel] = {
        bestModels.map(mp => mp._2)
    }

    def BestModelsWithMetrics: ArrayBuffer[(IModel, Map[StrategyMetric, Double])] = {
        return bestModels.map(bm => (bm._2, bm._3))
    }


    def Process(models: Seq[IModel]) = {

        models.foreach(model => {

            var tradingCases = Utils.GetTradingCases(model.trades);

            var metrics = estimatedMetrics(tradingCases);

            var est = metrics(optimizedFunctionName);
            if ((bestModels.length == 0 || est > bestModels(0)._1) && model.trades.length > minNumberOfTrades) {
                bestModels += ((est, model, metrics))
            }

            Estimates += new ExecutionEstimates(ExtractOptParams(model), metrics)

        })

        System.out.println("processed " + Estimates.length + " models ")

        bestModels.sortBy(a => a._1)


        while (bestModels.length > topModelsToKeep) {
            bestModels.remove(0);
        }
    }

    private def ExtractOptParams(model: IModel): Map[String, Int] = {
        return optParams map (s => (s, model.properties(s).toInt)) toMap;
    }
}
