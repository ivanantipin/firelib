package firelib.backtest

import firelib.common._

trait ReportProcessor {
    def estimates: Seq[ExecutionEstimates]

    def bestModels: Seq[IModel]

}
