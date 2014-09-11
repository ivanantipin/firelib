package firelib.common.marketstub

import firelib.common.config.InstrumentConfig

/**

 */
object defaultMarketStubFactory extends MarketStubFactory{
    override def apply(v1: InstrumentConfig): MarketStub = new MarketStubImpl(v1.ticker)
}
