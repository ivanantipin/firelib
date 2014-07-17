package firelib.domain
import firelib.backtest.StrategyMetric

import scala.collection.Map

class ExecutionEstimates(val OptParams: Map[String, Int], val MetricName2Value: Map[StrategyMetric, Double])
