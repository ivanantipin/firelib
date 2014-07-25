package firelib.backtest

import firelib.domain.ExecutionEstimates

/**
 * Created by ivan on 7/20/14.
 */
trait IReportProcessor {
    def Estimates: Seq[ExecutionEstimates]

    def BestModels: Seq[IModel]

}
