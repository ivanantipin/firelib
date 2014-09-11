package firelib.common.core

import java.time.Instant

import firelib.common.MarketDataListener
import firelib.common.interval.{IntervalServiceComponent, StepListener}
import firelib.common.mddistributor.MarketDataDistributorComponent

import scala.collection.mutable.ArrayBuffer

/**

 */
trait MarketDataPlayerComponent{

    this : BacktestEnvironmentComponent with IntervalServiceComponent with MarketDataDistributorComponent =>

    val marketDataPlayer = new MarketDataPlayer

    class MarketDataPlayer {


        private val stepListeners = new ArrayBuffer[StepListener]()

        def addListener(lsn: MarketDataListener) = tickerPlayers.foreach(_.addListener(lsn))

        def addStepListener(lst: StepListener) = stepListeners += lst

        def addStepListenerAtBeginning(lst: StepListener) = stepListeners.insert(0, lst)

        def step(chunkEndGmt: Instant): Boolean = {
            for (i <- 0 until tickerPlayers.length) {
                if (!tickerPlayers(i).readUntil(chunkEndGmt)) {
                    return false
                }
            }
            stepListeners.foreach(_.onStep(chunkEndGmt))
            return true
        }

        def getStepListeners(): Seq[StepListener] = stepListeners

        def close() = tickerPlayers.foreach(_.close())

        def play() {
            addStepListener(intervalService)
            addListener(marketDataDistributor)
            var dtGmtcur = bounds._1
            while (dtGmtcur.isBefore(bounds._2) && step(dtGmtcur)) {
                dtGmtcur = dtGmtcur.plusMillis(stepMs)
            }
        }

    }

}
