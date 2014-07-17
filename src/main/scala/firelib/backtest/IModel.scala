package firelib.backtest

import firelib.domain.Trade

trait IModel extends IStepListener {
    
    val properties: Map[String, String]

    val name: String

    def initModel(modelProps: Map[String, String], marketStubs: List[IMarketStub], distributor: IMarketDataDistributor)

    def trades: Seq[Trade]

    def hasValidProps: Boolean

    def stubs: Array[IMarketStub]

    def onBacktestEnd
}
