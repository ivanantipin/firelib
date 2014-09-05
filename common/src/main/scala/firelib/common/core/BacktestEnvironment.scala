package firelib.common.core

import firelib.common.interval.StepListener
import firelib.common.marketstub.MarketStub
import firelib.common.mddistributor.MarketDataDistributor

import firelib.common.model.Model

/**
 * environment that can be used to backtest one or many models
 */

trait BacktestEnvironment{

    val models : Seq[Model]
    def backtest()
    def bindModel(model : Model, stubs : Seq[MarketStub],  modelProps: Map[String, String])
    def mdDistr : MarketDataDistributor
    def stepListeners: Seq[StepListener]

}
