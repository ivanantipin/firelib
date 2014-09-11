package firelib.common.model

import firelib.common.Trade
import firelib.common.interval.StepListener
import firelib.common.marketstub.MarketStub
import firelib.common.mddistributor.MarketDataDistributor

/**

 */
trait Model extends StepListener {

    def properties: Map[String, String]

    def name: String

    def stubs: Seq[MarketStub]

    def trades: Seq[Trade]

    /**
     * init model method
     */
    def initModel(modelProps: Map[String, String], marketStubs: Seq[MarketStub], distributor: MarketDataDistributor)

    /**
     * sometimes properties become invalid during optimization variations
     * so we need to ignore such models
     */
    def hasValidProps(): Boolean

    /**
     * called after backtest end
     */
    def onBacktestEnd()
}
