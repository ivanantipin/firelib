package firelib.common.core

import java.time.Instant

import firelib.common.interval.StepListener
import firelib.common.marketstub.{BidAskUpdater, MarketStub}
import firelib.common.mddistributor.{MarketDataDistributor, MarketDataDistributorComponent}
import firelib.common.model.Model
import firelib.common.reader.ReaderToListenerAdapter

import scala.collection.mutable.ArrayBuffer

/**
 * Created by ivan on 9/4/14.
 */
trait BacktestEnvironmentComponent{

    this : MarketDataDistributorComponent with MarketDataPlayerComponent =>

    val bounds : (Instant,Instant)

    val stepMs : Int

    val tickerPlayers: Seq[ReaderToListenerAdapter]

    val env = new BacktestEnvironmentImpl

    class BacktestEnvironmentImpl() extends BacktestEnvironment{

        val models = new ArrayBuffer[Model]()

        def mdDistr : MarketDataDistributor = marketDataDistributor

        override def stepListeners: Seq[StepListener] = marketDataPlayer.getStepListeners

        def backtest(){
            marketDataPlayer.play()
            models.foreach(_.onBacktestEnd())
            marketDataPlayer.close()
        }

        def bindModel(model : Model, stubs : Seq[MarketStub],  modelProps: Map[String, String]): Unit = {
            model.initModel(modelProps, stubs, marketDataDistributor)
            if(model.hasValidProps()){
                marketDataPlayer.addStepListenerAtBeginning(model)
                bindStubs(stubs)
                models += model
            }
        }

        private def bindStubs(stubs : Seq[MarketStub]): Unit = {
            val updater = new BidAskUpdater(stubs)
            marketDataPlayer.addListener(updater)
            marketDataPlayer.addStepListener(updater)
        }


    }

}
