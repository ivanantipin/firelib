package firelib.common.misc

trait Topic[T] extends PubTopic[T] with SubTopic[T] {}

trait PubTopic[T]{
    def publish(t : T) : Unit
}

trait SubTopic[T]{
    def subscribe(lsn : T=>Unit) : Unit
}


