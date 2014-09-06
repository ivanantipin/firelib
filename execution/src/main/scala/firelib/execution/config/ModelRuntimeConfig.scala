package firelib.execution.config

import firelib.common.config.ModelConfig


class ModelRuntimeConfig {

    var modelConfig: ModelConfig = new ModelConfig()

    var gatewayType: String = _

    var runBacktest: Boolean = false

    var gatewayConfig: Map[String, String]=_

    var tradeLogDirectory: Option[String]=_

}
