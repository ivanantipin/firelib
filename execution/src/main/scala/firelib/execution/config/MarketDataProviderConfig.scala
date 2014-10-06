package firelib.execution.config

class MarketDataProviderConfig{

    /**
     * class of adapter, will be created with reflection
     * adapter need to implement TradeGate and MarketDataProvider
     */
    var providerClassName: String = _

    /**
     * config passed to trading gateway
     */
    var providerParams: Map[String, String]=_

}
