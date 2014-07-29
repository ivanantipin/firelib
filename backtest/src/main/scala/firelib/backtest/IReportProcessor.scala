package firelib.backtest

import firelib.common._

trait IReportProcessor {
    def Estimates: Seq[ExecutionEstimates]

    def BestModels: Seq[IModel]

}
