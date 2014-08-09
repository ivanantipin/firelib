package firelib.backtest

import firelib.common._

import scala.collection.Map
import scala.collection.mutable.ArrayBuffer

class ReportProcessor(val estimatedMetrics: (Seq[(Trade, Trade)]) => Map[StrategyMetric, Double], val optimizedFunctionName: StrategyMetric,

                      val optParams: Seq[String], val topModelsToKeep: Int = 3, val minNumberOfTrades: Int = 1, val removeOutlierTrades: Int = 2) extends IReportProcessor {


    private val bestModels = new ArrayBuffer[(Double, IModel, Map[StrategyMetric, Double])]()
    var Estimates = new ArrayBuffer[ExecutionEstimates]()

    def BestModels: Seq[IModel] = bestModels.map(_._2)

    def BestModelsWithMetrics: ArrayBuffer[(IModel, Map[StrategyMetric, Double])] = {
        return bestModels.map(bm => (bm._2, bm._3))
    }


    def process(models: Seq[IModel]) = {

        models.foreach(model => {

            val tradingCases = Utils.toTradingCases(model.trades)

            val metrics = estimatedMetrics(tradingCases)

            val est = metrics(optimizedFunctionName)
            if ((bestModels.length == 0 || est > bestModels(0)._1) && model.trades.length > minNumberOfTrades) {
                bestModels += ((est, model, metrics))
            }

            Estimates += new ExecutionEstimates(extractOptParams(model), metrics)

        })

        System.out.println("processed " + Estimates.length + " models ")

        bestModels.sortBy(_._1)


        while (bestModels.length > topModelsToKeep) {
            bestModels.remove(0)
        }
    }

    private def extractOptParams(model: IModel): Map[String, Int] = {
        return optParams map (s => (s, model.properties(s).toInt)) toMap
    }
}
