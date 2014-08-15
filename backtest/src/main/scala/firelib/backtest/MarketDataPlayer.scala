package firelib.backtest

import java.time.Instant

import firelib.common._

import scala.collection.mutable.ArrayBuffer


class MarketDataPlayer(val tickerPlayers: Seq[ReaderToListenerAdapter], val bounds : (Instant,Instant), stepMs : Int) {

    private val stepListeners = new ArrayBuffer[IStepListener]()

    def addListener(lsn: IMarketDataListener) = tickerPlayers.foreach(_.addListener(lsn))

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

    def close() = tickerPlayers.foreach(_.close())

    def play() {
        var dtGmtcur = bounds._1
        while (dtGmtcur.isBefore(bounds._2) && step(dtGmtcur)) {
            dtGmtcur = dtGmtcur.plusMillis(stepMs)
        }
    }

}
