package firelib.common.misc

import scala.collection.mutable.ArrayBuffer

class NonDurableTopic[T] extends Topic[T]{

    val listeners = new ArrayBuffer[T=>Unit]()

    def publish(t : T) : Unit = {
        listeners.foreach(_(t))
    }

    def subscribe(lsn : T=>Unit) : Unit = {
        listeners += lsn
    }
}
