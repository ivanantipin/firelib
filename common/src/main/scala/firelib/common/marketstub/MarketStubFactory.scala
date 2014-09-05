package firelib.common.marketstub

import firelib.common.config.TickerConfig

/**
 * Created by ivan on 9/4/14.
 */
trait MarketStubFactory extends (TickerConfig => MarketStub)
