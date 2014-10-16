package firelib.common.ticknorm

import firelib.domain.Tick

class NoOpTickToTick extends (Tick=>Tick){
    override def apply(v1: Tick): Tick = v1
}


