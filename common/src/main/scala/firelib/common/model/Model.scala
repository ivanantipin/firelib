package firelib.common.model

import firelib.common.interval.StepListener
import firelib.common.marketstub.MarketStub
import firelib.common.mddistributor.MarketDataDistributor
import firelib.common.Trade

/**
 * Created by ivan on 9/5/14.
 */
trait Model extends StepListener {

    def properties: Map[String, String]

    def name: String

    def stubs: Seq[MarketStub]

    def trades: Seq[Trade]

    def initModel(modelProps: Map[String, String], marketStubs: Seq[MarketStub], distributor: MarketDataDistributor)

    /**
     * sometimes properties become invalid during optimization variations
     * so we need to ignore such models
     */
    def hasValidProps(): Boolean

    def onBacktestEnd()
}
