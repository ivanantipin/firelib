package firelib.common.marketstub

import firelib.common.config.InstrumentConfig

/**

 */
trait MarketStubFactory extends (InstrumentConfig => MarketStub)
