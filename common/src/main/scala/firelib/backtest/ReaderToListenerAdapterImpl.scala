package firelib.backtest

import java.time.Instant

import firelib.common.{IMarketDataListener, ISimpleReader}
import firelib.domain.Timed

import scala.collection.mutable.ArrayBuffer


class ReaderToListenerAdapterImpl[T <: Timed](val reader : ISimpleReader[T],
                                     val idx : Int,
                                     val bridgeFunction : (IMarketDataListener,Int, T, T)=>Unit ) extends ReaderToListenerAdapter{

    private val listeners = new ArrayBuffer[IMarketDataListener]()

    override def addListener(lsn: IMarketDataListener): Unit = listeners += lsn

    override def readUntil(chunkEndGmt:Instant): Boolean = {
        while (!reader.current.DtGmt.isAfter(chunkEndGmt)) {
            val current = reader.current
            if (!reader.read) {
                return false
            }
            listeners.foreach(bridgeFunction(_,idx,current,reader.current))
        }
        return true
    }

    override def close() = reader.close()
}
