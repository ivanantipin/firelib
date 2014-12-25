package firelib.common.misc

trait Topic[T] extends PubTopic[T] with SubTopic[T] { }

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
    def subscribe(lsn : T=>Unit) : TopicSubscription

    def lift[B](mf : T=>Seq[B]) : SubTopic[B] = {
        val ret = new NonDurableTopic[B]
        subscribe(t=>{
            for(oo<-mf(t)){
                ret.publish(oo)
            }
        })
        ret
    }

    def map[B](mf : T=>B) : SubTopic[B] = {
        val ret = new NonDurableTopic[B]
        subscribe(t=>ret.publish(mf(t)))
        ret
    }

    def filter(flt: (T) => Boolean): SubTopic[T] = {
        val ret = new NonDurableTopic[T]()
        subscribe(tt=>{
            if(flt(tt)){
                ret.publish(tt)
            }
        })
        ret
    }


}


