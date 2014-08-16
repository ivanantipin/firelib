package firelib.common

trait IModel extends IStepListener {
    
    def properties: Map[String, String]

    def name: String

    def stubs: Seq[IMarketStub]

    def trades: Seq[Trade]

    def initModel(modelProps: Map[String, String], marketStubs: Seq[IMarketStub], distributor: IMarketDataDistributor)

    /**
     * sometimes properties become invalid during optimization variations
     * so we need to ignore such models
     */
    def hasValidProps(): Boolean

    def onBacktestEnd()
}
