package firelib.execution.config

import firelib.common.config.ModelConfig
import firelib.common.ticknorm.NoOpTickToTick


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
     * specify if model need to run backtest before start as often we need to init
     * different kinds of quantiles etc
     */
    var runBacktest: Boolean = false



    var tradeGateConfig : TradeGateConfig =_

    var marketDataProviderConfig : MarketDataProviderConfig =_

    var tickToTickFuncClass : String = classOf[NoOpTickToTick].getName

    /**
     * directory to log trades
     */
    var tradeLogDirectory: Option[String]=_


}
