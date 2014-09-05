package firelib.common.marketstub

import firelib.common.config.TickerConfig

/**
 * Created by ivan on 9/5/14.
 */
trait MarketStubFactoryComponent {
    val marketStubFactory : (TickerConfig=>MarketStub) = defaultMarketStubFactory
}
