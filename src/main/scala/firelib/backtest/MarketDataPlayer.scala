package firelib.backtest

import firelib.domain.{IntervalEnum, Timed}
import org.joda.time.DateTime

import scala.collection.mutable.ArrayBuffer


trait ISimpleReader {

    def Dispose() = ???

    def UpdateTimeZoneOffset() = ???

    def CurrentQuote: Timed

    def Read: Boolean

}

class MarketDataPlayer(val readers: Array[ISimpleReader]) {

    private val stepListeners = new ArrayBuffer[IStepListener]()


    val quoteListeners = new ArrayBuffer[IQuoteListener[_ <: Timed]]();

    val nextQuoteListeners = new ArrayBuffer[IQuoteListener[_ <: Timed]]();

    //keep it local to do not mess with runtime interval service
    private val intervalService = new IntervalService();


    intervalService.AddListener(IntervalEnum.Min240, (dt) => {
        for (reader <- readers) {
            reader.UpdateTimeZoneOffset();
        }
    })


    def GetMarketDataListeners: Array[IQuoteListener[_ <: Timed]] = {
        return quoteListeners.toArray
    }

    def GetNextQuoteListeners: Array[IQuoteListener[_ <: Timed]] = {
        return nextQuoteListeners.toArray
    }


    def GetStepListeners: Array[IStepListener] = {
        return stepListeners.toArray
    }

    def AddQuoteListener(listener: IQuoteListener[_ <: Timed]) = {
        quoteListeners += listener

    }

    def AddNextQuoteListener(listener: IQuoteListener[_ <: Timed]) = {
        nextQuoteListeners += listener
    }

    def AddStepListener(lst: IStepListener) = {
        stepListeners += lst
    }

    def AddStepListenerAtBeginning(lst: IStepListener) = {
        stepListeners.insert(0, lst)
    }

    def Step(chunkEndGmt: DateTime): Boolean = {
        intervalService.OnStep(chunkEndGmt);

        for (i <- 0 until readers.length) {

            while (readers(i).CurrentQuote.DtGmt.isBefore(chunkEndGmt)) {
                var recordQuote = readers(i).CurrentQuote;
                quoteListeners.foreach(ql => ql.AddQuote(i, recordQuote));
                if (!readers(i).Read) {
                    return false;
                }
            }

            nextQuoteListeners.foreach(ql => ql.AddQuote(i, readers(i).CurrentQuote));
        }

        for (lst <- stepListeners) lst.OnStep(chunkEndGmt);

        return true;
    }

    def Dispose() {
        for (rd <- readers) rd.Dispose()
    }
}
