package firelib.common.marketstub

import firelib.common.config.InstrumentConfig

/**

 */
trait MarketStubFactoryComponent {
    val marketStubFactory : (InstrumentConfig=>MarketStub) = defaultMarketStubFactory
}
