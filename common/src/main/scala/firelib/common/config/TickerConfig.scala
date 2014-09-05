package firelib.common.config

import firelib.common.MarketDataType

/**
 * Created by ivan on 9/5/14.
 */
case class TickerConfig(val ticker: String, val path: String, val mdType: MarketDataType) {}
