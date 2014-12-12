package firelib.common.config

import firelib.common.core.BacktestMode
import firelib.common.interval.Interval
import firelib.common.misc.utils
import firelib.common.model.Model
import firelib.common.report.StrategyMetric
import firelib.common.ticknorm.NormBidAskTickFunc

import scala.collection.mutable._

/**
 * keeps configuration for model backtest
 */
class ModelBacktestConfig {
    /**
     * instruments configuration
     */
    val instruments = ArrayBuffer[InstrumentConfig]()

    var startDateGmt: String = _

    var endDate: String = _

    var dataServerRoot: String = _

    var reportTargetPath: String = _
    
    var modelClassName: String = _

    var tickToTickFuncClass : String = classOf[NormBidAskTickFunc].getName

    var precacheMarketData : Boolean = false

    var networkSimulatedDelayMs = 30l

    /**
     * params passed to model apply method
     * can not be optimized
     */
    val modelParams = HashMap[String, String]()

    val optConfig : OptimizationConfig= new OptimizationConfig

    var backtestMode = BacktestMode.SimpleRun

    /**
     * step of backtest specifies frequency when ohlc bars checked to generate
     * can affect performance if interval is small
     */
    var stepInterval = Interval.Sec1


    val calculatedMetrics = List(
        StrategyMetric.Pf,
        StrategyMetric.Pnl,
        StrategyMetric.Sharpe,
        StrategyMetric.AvgPnl
    )

    def newModelInstance() : Model ={
        return utils.instanceOfClass[Model](modelClassName)
    }

}
