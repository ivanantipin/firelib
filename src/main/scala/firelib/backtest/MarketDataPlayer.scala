package firelib.backtest

import firelib.domain.Interval
import org.joda.time.DateTime

import scala.collection.mutable.ArrayBuffer



class MarketDataPlayer(val tickerPlayers: Seq[TickerMdPlayer]) {



    private val stepListeners = new ArrayBuffer[IStepListener]()

    //keep it local to do not mess with runtime interval service
    private val intervalService = new IntervalService();


    intervalService.AddListener(Interval.Min240, (dt) => {
        for (reader <- tickerPlayers) {
            reader.UpdateTimeZoneOffset()
        }
    })


    def AddListener(idx : Int, lsn : IMarketDataListener) = {
        tickerPlayers(idx).addListener(lsn)
    }

    def AddListenerForAll(lsn : IMarketDataListener) = {
        for(i <- tickerPlayers.length)
            tickerPlayers(i).addListener(lsn)
    }

    def AddStepListener(lst: IStepListener) = {
        stepListeners += lst
    }

    def AddStepListenerAtBeginning(lst: IStepListener) = {
        stepListeners.insert(0, lst)
    }

    def Step(chunkEndGmt: DateTime): Boolean = {
        intervalService.OnStep(chunkEndGmt);

        for (i <- 0 until tickerPlayers.length) {
            if(tickerPlayers(i).ReadUntil(chunkEndGmt)){
                return false
            }
        }

        for (lst <- stepListeners) lst.OnStep(chunkEndGmt);
        return true;
    }

    def getStepListeners(): Seq[IStepListener] = stepListeners

    def Dispose() {
        for (rd <- tickerPlayers) rd.Dispose()
    }

}
