package firelib.common

trait IModel extends IStepListener {
    
    def properties: Map[String, String]

    def name: String

    def stubs: Seq[IMarketStub]

    def trades: Seq[Trade]

    def initModel(modelProps: Map[String, String], marketStubs: Seq[IMarketStub], distributor: IMarketDataDistributor)

    def hasValidProps(): Boolean

    def onBacktestEnd()
}
