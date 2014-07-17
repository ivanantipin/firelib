package firelib.domain


object ResearchModeEnum {
    val SimpleRun = new ResearchMode("SimpleRun")
    val FwdTesting = new ResearchMode("FwdTesting")
    val InOutSample = new ResearchMode("InOutSample")
}

sealed class ResearchMode(val Name: String) {
    override def toString: String = Name
}
