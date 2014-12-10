package firelib.common.core

import java.time.{Duration, Instant}

import firelib.common.misc.Topic
import firelib.domain.Timed

import scala.collection.mutable
import scala.language.{implicitConversions, postfixOps}

class WindowSlicer[T <: Timed](val out : Topic[T], val in : Topic[T], val events: Topic[Any], val dur : Duration) {

    private val queue = new mutable.Queue[T]()

    var writeBefore: Instant = Instant.MIN

    events.subscribe(t => {
        updateWriteBefore()
        checkTail()
    })

    in.subscribe(oh => {
        queue += oh
        checkTail()
    })

    def checkTail() = {
        while (queue.nonEmpty && queue.head.DtGmt.isBefore(writeBefore)) {
            out.publish(queue.dequeue())
        }
        while (queue.nonEmpty && queue.last.DtGmt.getEpochSecond - queue.head.DtGmt.getEpochSecond > dur.getSeconds) {
            queue.dequeue()
        }
    }

    def updateWriteBefore() {
        if(queue.nonEmpty)
            writeBefore = queue.last.DtGmt.plus(dur)
    }
}
