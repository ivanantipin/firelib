package firelib.common

import scala.collection.mutable._

class ModelConfig {
    
    val tickerConfigs = ArrayBuffer[TickerConfig]()

    var startDateGmt: String = _

    var endDate: String = _

    var dataServerRoot: String = _

    var reportTargetPath: String = _
    
    var modelClassName: String = _

    /**
     * params passed to model apply method
     * can not be optimized
     */
    val customParams = HashMap[String, String]()


    /**
     * params passed to model applyProperties method
     * for InOutSample mode only
     */
    val optParams = new ArrayBuffer[OptimizedParameter]

    /**
     * number of model instances that backtested on one thread / one market data play in one go
     *
     */
    var optBatchSize = 500

    /**
     * number of threads used for optimization
     * make sense to do <= number of cores
     * for InOutSample mode only
     */
    var optThreadNumber = 1

    var optMinNumberOfTrades = 1

    var optimizedPeriodDays = -1

    var backtestMode = BacktestMode.SimpleRun

    /**
     * step of backtest specifies frequency when ohlc bars checked to generate
     * also in that step ticks supplied into model in batches for whole period
     * can affect performance if interval is small
     * for example for minutes market data does not make sense to do less than 1 Min interval
     */
    var backtestStepInterval = Interval.Sec1

    /**
     * optimization metrics for InOutSample mode only
     */
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

    def newModelInstance() : IModel ={
        return Class.forName(modelClassName).newInstance().asInstanceOf[IModel]
    }

}
