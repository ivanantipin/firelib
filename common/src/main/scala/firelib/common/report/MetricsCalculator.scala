package firelib.common.report

import firelib.common.Trade

/**
 * Created by ivan on 9/5/14.
 */
trait MetricsCalculator extends ((Seq[(Trade, Trade)]) => Map[StrategyMetric, Double])
