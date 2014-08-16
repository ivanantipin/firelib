package firelib.backtest

import firelib.common._

import scala.collection.mutable.ArrayBuffer
import scala.collection.{Map, mutable}

class ReportProcessor(val metricsCalculator : MetricsCalculator, val optimizedFunctionName: StrategyMetric,

                      val optParams: Seq[String], val topModelsToKeep: Int = 3, val minNumberOfTrades: Int = 1, val removeOutlierTrades: Int = 2) {


    type ModelStat = (Double, IModel, Map[StrategyMetric, Double])

    private val bestModels_ = new mutable.PriorityQueue[ModelStat]()(Ordering.by((ms : ModelStat) => ms._1 ).reverse)

    var estimates = new ArrayBuffer[ExecutionEstimates]()

    def bestModels: Seq[IModel] = bestModels_.map(_._2).toList


    def bestModelsWithMetrics: Seq[(IModel, Map[StrategyMetric, Double])] = {
        return bestModels_.map(bm => (bm._2, bm._3)).toList
    }

    def process(models: Seq[IModel]) = {

        models.foreach(model => {

            val tradingCases = utils.toTradingCases(model.trades)

            val metrics = metricsCalculator(tradingCases)

            val est = metrics(optimizedFunctionName)

            bestModels_ += ((est, model, metrics))
            if(bestModels_.length > topModelsToKeep)
                bestModels_.dequeue()

            estimates += new ExecutionEstimates(extractOptParams(model), metrics)

        })
        System.out.println("processed " + estimates.length + " models ")

    }

    private def extractOptParams(model: IModel): Map[String, Int] = {
        return optParams map (s => (s, model.properties(s).toInt)) toMap
    }
}