package firelib.robot

import firelib.backtest.ModelConfig


class ModelRuntimeConfig {

    var modelConfig: ModelConfig =_

    var GatewayType: String = _

    var RunBacktest: Boolean = false;

    var GatewayConfig: Map[String, String]=_

    var tradeLogDirectory: Option[String]=_

    val IbContractMapping = Map[String, String](
        "EURUSD" -> "Symbol=EUR;SecType=CASH;Currency=USD;Exchange=IDEALPRO",
        "SPY" -> "Symbol=SPY;SecType=STK;Currency=USD;Exchange=ARCA",
        "DXJ" -> "Symbol=DXJ;SecType=STK;Currency=USD;Exchange=ARCA",
        "AMLP" -> "Symbol=AMLP;SecType=STK;Currency=USD;Exchange=ARCA",
        "QQQ" -> "Symbol=QQQ;SecType=STK;Currency=USD;Exchange=ARCA",
        "EEM" -> "Symbol=EEM;SecType=STK;Currency=USD;Exchange=ARCA"
    )
}
