package firelib.robot


class ModelRuntimeConfig {

    var modelConfig: ModelConfig

    var GatewayType: String = _

    var RunBacktest: Boolean = false;

    var GatewayConfig: Map[String, String]

    var TradeLogDirectory: String

    val IbContractMapping = Map[String, String](
        "EURUSD" -> "Symbol=EUR;SecType=CASH;Currency=USD;Exchange=IDEALPRO",
        "SPY" -> "Symbol=SPY;SecType=STK;Currency=USD;Exchange=ARCA",
        "DXJ" -> "Symbol=DXJ;SecType=STK;Currency=USD;Exchange=ARCA",
        "AMLP" -> "Symbol=AMLP;SecType=STK;Currency=USD;Exchange=ARCA",
        "QQQ" -> "Symbol=QQQ;SecType=STK;Currency=USD;Exchange=ARCA",
        "EEM" -> "Symbol=EEM;SecType=STK;Currency=USD;Exchange=ARCA"
    )
}
