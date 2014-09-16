package firelib.execution.config

import firelib.common.config.ModelConfig


/**
 * configuration for execution environment
 */
class ModelRuntimeConfig {

    /**
     * model config, same as used for backtesting
     * to init model on start
     */
    var modelConfig: ModelConfig = new ModelConfig()

    /**
     * class of adapter, will be created with reflection
     * adapter need to implement TradeGate and MarketDataProvider
     */
    var gatewayType: String = _

    /**
     * specify if model need to run backtest before start as often we need to init
     * different kinds of quantiles etc
     */
    var runBacktest: Boolean = false

    /**
     * config passed to trading gateway
     */
    var gatewayConfig: Map[String, String]=_

    /**
     * directory to log trades
     */
    var tradeLogDirectory: Option[String]=_

}
