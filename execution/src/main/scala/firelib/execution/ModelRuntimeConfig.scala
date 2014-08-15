package firelib.robot

import firelib.common._


class ModelRuntimeConfig {

    var modelConfig: ModelConfig = new ModelConfig()

    var gatewayType: String = _

    var runBacktest: Boolean = false

    var gatewayConfig: Map[String, String]=_

    var tradeLogDirectory: Option[String]=_

    val ibContractMapping = Map[String, String](
        "EURUSD" -> "m_symbol=EUR;m_secType=CASH;m_currency=USD;m_exchange=IDEALPRO",
        "SPY" -> "m_symbol=SPY;m_secType=STK;m_currency=USD;m_exchange=ARCA",
        "DXJ" -> "m_symbol=DXJ;m_secType=STK;m_currency=USD;m_exchange=ARCA",
        "AMLP" -> "m_symbol=AMLP;m_secType=STK;m_currency=USD;m_exchange=ARCA",
        "QQQ" -> "m_symbol=QQQ;m_secType=STK;m_currency=USD;m_exchange=ARCA",
        "EEM" -> "m_symbol=EEM;m_secType=STK;m_currency=USD;m_exchange=ARCA"
    )

}
