package firelib.robot

import firelib.common._


class ModelRuntimeConfig {

    var modelConfig: ModelConfig =_

    var GatewayType: String = _

    var runBacktest: Boolean = false

    var gatewayConfig: Map[String, String]=_

    var tradeLogDirectory: Option[String]=_

    val IbContractMapping = Map[String, String](
        "EURUSD" -> "Symbol=EURSecType=CASHCurrency=USDExchange=IDEALPRO",
        "SPY" -> "Symbol=SPYSecType=STKCurrency=USDExchange=ARCA",
        "DXJ" -> "Symbol=DXJSecType=STKCurrency=USDExchange=ARCA",
        "AMLP" -> "Symbol=AMLPSecType=STKCurrency=USDExchange=ARCA",
        "QQQ" -> "Symbol=QQQSecType=STKCurrency=USDExchange=ARCA",
        "EEM" -> "Symbol=EEMSecType=STKCurrency=USDExchange=ARCA"
    )
}
