package firelib.common.config

import firelib.common.opt.OptimizedParameter
import firelib.common.report.StrategyMetric

import scala.collection.mutable.ArrayBuffer

/**
 * config that contains all optimization parameters
 */
class OptimizationConfig{

    /**
     * optimization parameters
     */
    val params = new ArrayBuffer[OptimizedParameter]

    /**
     * number of model instances that backtested in one thread in one market data replay
     */
    var batchSize = 500

    /**
     * number of threads used for optimization
     * make sense to do <= number of cores
     */
    var threadsNumber = 1


    var minNumberOfTrades = 1

    /**
     * number of days to optimize params before out of sample run
     */
    var optimizedPeriodDays = -1

    /**
     * optimization metrics
     */
    var optimizedMetric = StrategyMetric.Sharpe


}
