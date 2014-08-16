package firelib.common

import scala.collection.mutable._

class ModelConfig {
    val tickerConfigs = ArrayBuffer[TickerConfig]()

    var startDateGmt: String = _

    var endDate: String = _

    var dataServerRoot: String = _

    var reportRoot: String = _

    var className: String = _

    var frequencyIntervalId: String = _

    val customParams = HashMap[String, String]()

    val optParams = new ArrayBuffer[OptimizedParameter]

    var optBatchSize = 500

    var optThreadNumber = 1

    var optMinNumberOfTrades = 1

    var optimizedPeriodDays = -1

    var mode = ResearchMode.SimpleRun

    def interval = Interval.resolveFromName(frequencyIntervalId)

    var optimizedMetric = StrategyMetric.Sharpe

    val calculatedMetrics = List(
        StrategyMetric.Pf,
        StrategyMetric.Pnl,
        StrategyMetric.Sharpe,
        StrategyMetric.AvgPnl
    )


    def addCustomParam(param: String, value: String): ModelConfig = {
        customParams(param) = value
        this
    }

    def newInstance() : IModel ={
        return Class.forName(className).newInstance().asInstanceOf[IModel]
    }

}
