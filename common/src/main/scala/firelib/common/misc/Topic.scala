package firelib.common.misc

trait Topic[T] extends PubTopic[T] with SubTopic[T] {}

object Topic {
    def apply[T](subs : T=>Unit) : Topic[T] = {
        val ret = new NonDurableTopic[T]
        ret.subscribe(subs)
        ret
    }
}

trait PubTopic[T]{
    def publish(t : T) : Unit
}

trait SubTopic[T]{
    def subscribe(lsn : T=>Unit) : Unit
}


