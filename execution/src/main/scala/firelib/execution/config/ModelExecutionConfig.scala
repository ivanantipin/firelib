package firelib.execution.config

import firelib.common.config.ModelBacktestConfig
import firelib.common.ticknorm.NormBidAskTickFunc


/**
 * configuration for execution environment
 */
class ModelExecutionConfig {

    /**
     * model config, same as used for backtesting
     * to init model on start
     */
    var backtestConfig: ModelBacktestConfig = new ModelBacktestConfig()

    /**
     * specify if model need to run backtest before start as often we need to init
     * different kinds of quantiles etc
     */
    var runBacktestBeforeStrategyRun: Boolean = false


    var tradeGateConfig : TradeGateConfig =_

    var marketDataProviderConfig : MarketDataProviderConfig =_

    /**
     * function that transform ticks from market data provider
     * as often some fields can be missing like last and only bid/ask present
     */
    var tickToTickFuncClass : String = classOf[NormBidAskTickFunc].getName

    /**
     * directory to log trades
     */
    var tradeLogDirectory: Option[String]=None


}
