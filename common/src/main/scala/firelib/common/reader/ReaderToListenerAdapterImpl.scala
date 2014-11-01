package firelib.common.reader

import java.time.Instant

import firelib.common.MarketDataListener
import firelib.domain.Timed

import scala.collection.mutable.ArrayBuffer


class ReaderToListenerAdapterImpl[T <: Timed](val reader : MarketDataReader[T],
                                     val idx : Int,
                                     val bridgeFunction : (MarketDataListener,Int, T, T)=>Unit ) extends ReaderToListenerAdapter{

    private val listeners = new ArrayBuffer[MarketDataListener]()

    override def addListener(lsn: MarketDataListener): Unit = listeners += lsn

    override def readUntil(chunkEndGmt:Instant): Boolean = {
        while (!reader.current.DtGmt.isAfter(chunkEndGmt)) {
            val current = reader.current
            if (!reader.read()) {
                return false
            }
            listeners.foreach(bridgeFunction(_,idx,current,reader.current))
        }
        return true
    }

    override def close() = reader.close()
}
