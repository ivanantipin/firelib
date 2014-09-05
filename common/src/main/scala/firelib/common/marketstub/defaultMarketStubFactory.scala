package firelib.common.marketstub

import firelib.common.config.TickerConfig
import firelib.common.marketstub.MarketStubFactory

/**
 * Created by ivan on 9/4/14.
 */
object defaultMarketStubFactory extends MarketStubFactory{
    override def apply(v1: TickerConfig): MarketStub = new MarketStubImpl(v1.ticker)
}
