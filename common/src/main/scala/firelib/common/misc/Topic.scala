package firelib.common.misc

trait Topic[T]{
    def publish(t : T) : Unit
    def subscribe(lsn : T=>Unit) : Unit
}
