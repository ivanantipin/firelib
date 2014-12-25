package firelib.common.misc

import scala.collection.mutable.ArrayBuffer


trait TopicSubscription{
    def unsubscribe()
}

class NonDurableTopic[T] extends Topic[T]{

    val listeners = new ArrayBuffer[T=>Unit]()

    def publish(t : T) : Unit = {
        listeners.foreach(_(t))
    }

    def subscribe(lsn : T=>Unit) : TopicSubscription = {
        listeners += lsn
        new TopicSubscription {
            override def unsubscribe(): Unit = listeners -= lsn
        }
    }

}
