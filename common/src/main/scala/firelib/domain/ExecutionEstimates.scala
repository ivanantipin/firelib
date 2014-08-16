package firelib.common
import scala.collection.Map

class ExecutionEstimates(val optParams: Map[String, Int], val metricToValue: Map[StrategyMetric, Double])
