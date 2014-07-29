package firelib.backtest

import java.time.Instant

import firelib.common._

import scala.collection.mutable.ArrayBuffer


class MarketDataPlayer(val tickerPlayers: Seq[TickerMdPlayer]) {



    private val stepListeners = new ArrayBuffer[IStepListener]()

    //keep it local to do not mess with runtime interval service
    private val intervalService = new IntervalService();


    intervalService.AddListener(Interval.Min240, (dt) => tickerPlayers.foreach(_.UpdateTimeZoneOffset()))


    def AddListener(idx : Int, lsn : IMarketDataListener) = tickerPlayers(idx).addListener(lsn)

    def AddListenerForAll(lsn : IMarketDataListener) = tickerPlayers.foreach(_.addListener(lsn))


    def AddStepListener(lst: IStepListener) = stepListeners += lst

    def AddStepListenerAtBeginning(lst: IStepListener) = stepListeners.insert(0, lst)

    def Step(chunkEndGmt:Instant): Boolean = {
        intervalService.OnStep(chunkEndGmt);
        for (i <- 0 until tickerPlayers.length) {
            if(tickerPlayers(i).ReadUntil(chunkEndGmt)){
                return false
            }
        }
        stepListeners.foreach(_.OnStep(chunkEndGmt))
        return true;
    }

    def getStepListeners(): Seq[IStepListener] = stepListeners

    def Dispose() = tickerPlayers.foreach(_.Dispose())


}
