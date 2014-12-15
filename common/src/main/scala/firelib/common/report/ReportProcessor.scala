package firelib.common.report

import firelib.common.core.ModelOutput
import firelib.common.misc.utils
import firelib.common.model.Model

import scala.collection.mutable.ArrayBuffer
import scala.collection.{Map, mutable}

class ReportProcessor(val metricsCalculator : MetricsCalculator, val optimizedFunctionName: StrategyMetric,
                      val optParams: Seq[String], val topModelsToKeep: Int = 3, val minNumberOfTrades: Int = 1, val removeOutlierTrades: Int = 2) {


    case class ModelStat (optMetric : Double, output : ModelOutput, metrics : Map[StrategyMetric, Double])

    private val bestModels_ = new mutable.PriorityQueue[ModelStat]()(Ordering.by((ms : ModelStat) => ms.optMetric ).reverse)

    var estimates = new ArrayBuffer[ExecutionEstimates]()

    def bestModels: Seq[ModelOutput] = bestModels_.map(_.output).toList


    def bestModelsWithMetrics: Seq[(Model, Map[StrategyMetric, Double])] = {
        return bestModels_.map(bm => (bm.output.model, bm.metrics)).toList
    }

    def process(models: Seq[ModelOutput]) = {

        val filtered: Seq[ModelOutput] = models.filter(_.trades.size >= minNumberOfTrades)

        println(s"model processed ${models.length} models met min trades count criteria ${filtered.length}")

        filtered.foreach(model => {

            val tradingCases = utils.toTradingCases(model.trades)

            val metrics = metricsCalculator(tradingCases)

            val est = metrics(optimizedFunctionName)

            bestModels_ += new ModelStat(est, model, metrics)
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
