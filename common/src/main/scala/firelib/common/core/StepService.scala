package firelib.common.core

import java.time.Instant

import firelib.common.interval.StepListener

trait StepService{

    def onStep(dtGmt : Instant) : Unit

    def listen(lsn : StepListener) : Unit

    def priorityListen(lsn : StepListener) : Unit

}
