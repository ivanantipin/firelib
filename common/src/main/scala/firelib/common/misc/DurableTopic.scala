package firelib.common.misc

import scala.collection.mutable.ArrayBuffer

class DurableTopic[T] extends Topic[T]{

    val listeners = new ArrayBuffer[T=>Unit]()

    val msgs = new ArrayBuffer[T](2)

    def publish(t : T) : Unit = {
        listeners.foreach(_(t))
        msgs += t
    }

    def subscribe(lsn : T=>Unit) : TopicSubscription= {
        listeners += lsn
        msgs.foreach(lsn(_))
        new TopicSubscription {
            override def unsubscribe(): Unit = listeners -= lsn
        }

    }
}
