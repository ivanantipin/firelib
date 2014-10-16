package firelib.common.core

object BacktestMode {
    val SimpleRun = new BacktestMode("SimpleRun")
    val FwdTesting = new BacktestMode("FwdTesting")
    val InOutSample = new BacktestMode("InOutSample")
}

sealed case class BacktestMode private (val name: String) {
    override def toString: String = name
}
