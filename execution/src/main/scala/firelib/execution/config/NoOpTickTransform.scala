package firelib.execution.config

import firelib.domain.Tick

class NoOpTickTransform extends (Tick=>Tick){
    override def apply(v1: Tick): Tick = return v1
}