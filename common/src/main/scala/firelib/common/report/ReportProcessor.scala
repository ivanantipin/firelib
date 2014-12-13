package firelib.common.report

import firelib.common.core.ModelOutput
import firelib.common.misc.utils
import firelib.common.model.Model

import scala.collection.mutable.ArrayBuffer
import scala.collection.{Map, mutable}

class ReportProcessor(val metricsCalculator : MetricsCalculator, val optimizedFunctionName: StrategyMetric,
                      val optParams: Seq[String], val topModelsToKeep: Int = 3, val minNumberOfTrades: Int = 1, val removeOutlierTrades: Int = 2) {


    type ModelStat = (Double, Model, Map[StrategyMetric, Double])

    private val bestModels_ = new mutable.PriorityQueue[ModelStat]()(Ordering.by((ms : ModelStat) => ms._1 ).reverse)

    var estimates = new ArrayBuffer[ExecutionEstimates]()

    def bestModels: Seq[Model] = bestModels_.map(_._2).toList


    def bestModelsWithMetrics: Seq[(Model, Map[StrategyMetric, Double])] = {
        return bestModels_.map(bm => (bm._2, bm._3)).toList
    }

    def process(models: Seq[ModelOutput]) = {

        val filtered: Seq[ModelOutput] = models.filter(_.trades.size >= minNumberOfTrades)

        println(s"model processed ${models.length} models met min trades count criteria ${filtered.length}")

        filtered.foreach(model => {

            val tradingCases = utils.toTradingCases(model.trades)

            if(model.model.properties("end.min") == "1370" && model.model.properties("start.min") == "1120"){
                println()
            }

            val metrics = metricsCalculator(tradingCases)

            val est = metrics(optimizedFunctionName)

            bestModels_ += ((est, model.model, metrics))
            if(bestModels_.length > topModelsToKeep)
                bestModels_.dequeue()

            estimates += new ExecutionEstimates(extractOptParams(model.model), metrics)

        })
        println(s"total model complete ${estimates.length}")

    }

    private def extractOptParams(model: Model): Map[String, Int] = {
        return optParams map (s => (s, model.properties(s).toInt)) toMap
    }
}
