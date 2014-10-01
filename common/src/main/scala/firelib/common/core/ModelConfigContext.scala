package firelib.common.core

import firelib.common.config.ModelConfig

/**
 * component of BacktestEnvironment factory for dependency injection
 */

trait ModelConfigContext{
    val modelConfig : ModelConfig
}

