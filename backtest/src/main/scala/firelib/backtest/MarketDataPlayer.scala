package firelib.backtest

import java.time.Instant

import firelib.common._

import scala.collection.mutable.ArrayBuffer


class MarketDataPlayer(val tickerPlayers: Seq[TickerMdPlayer]) {

    private val stepListeners = new ArrayBuffer[IStepListener]()

    def addListener(idx: Int, lsn: IMarketDataListener) = tickerPlayers(idx).addListener(lsn)

    def addListenerForAll(lsn: IMarketDataListener) = tickerPlayers.foreach(_.addListener(lsn))


    def addStepListener(lst: IStepListener) = stepListeners += lst

    def addStepListenerAtBeginning(lst: IStepListener) = stepListeners.insert(0, lst)

    def step(chunkEndGmt: Instant): Boolean = {
        for (i <- 0 until tickerPlayers.length) {
            if (!tickerPlayers(i).readUntil(chunkEndGmt)) {
                return false
            }
        }
        stepListeners.foreach(_.onStep(chunkEndGmt))
        return true
    }

    def getStepListeners(): Seq[IStepListener] = stepListeners

    def Dispose() = tickerPlayers.foreach(_.Dispose())


}
