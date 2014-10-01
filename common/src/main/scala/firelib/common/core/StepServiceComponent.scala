package firelib.common.core

import java.time.Instant

import firelib.common.interval.StepListener

import scala.collection.mutable.ArrayBuffer

trait StepServiceComponent{

    val stepService : StepService = new StepServiceImpl

    class StepServiceImpl extends StepService{

        private val listeners = new ArrayBuffer[StepListener]

        override def onStep(dtGmt: Instant): Unit = listeners.foreach(_.onStep(dtGmt))

        override def listen(lsn: StepListener): Unit = listeners += lsn

        override def priorityListen(lsn: StepListener): Unit = listeners.prepend(lsn)
    }
}
