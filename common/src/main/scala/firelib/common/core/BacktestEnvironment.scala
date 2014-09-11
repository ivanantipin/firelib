package firelib.common.core

import firelib.common.interval.StepListener
import firelib.common.marketstub.MarketStub
import firelib.common.mddistributor.MarketDataDistributor
import firelib.common.model.Model

/**
 * environment object that can be used to backtest one or many models
 * it is ONE time usage - so one you called backtest you can not call it anymore
 */
trait BacktestEnvironment{

    val models : Seq[Model]
    def backtest()
    def bindModel(model : Model, stubs : Seq[MarketStub],  modelProps: Map[String, String])
    def distributor : MarketDataDistributor
    def stepListeners: Seq[StepListener]

}
