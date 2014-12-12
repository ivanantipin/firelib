package firelib.common.misc

import java.time.{Duration, Instant}

import firelib.domain.Timed

import scala.collection.mutable
import scala.language.{implicitConversions, postfixOps}

class WindowSlicer[T <: Timed](val out : PubTopic[T], val in : SubTopic[T], val events: SubTopic[Any], val dur : Duration) {

    private val queue = new mutable.Queue[T]()

    var writeBefore: Instant = Instant.MIN

    var lastTime = Instant.MIN

    events.subscribe(t => {
        updateWriteBefore()
        checkTail()
    })

    in.subscribe(oh => {
        lastTime = oh.time
        queue += oh
        checkTail()
    })

    def checkTail() = {
        while (queue.nonEmpty && queue.head.time.isBefore(writeBefore)) {
            out.publish(queue.dequeue())
        }
        while (queue.nonEmpty && queue.last.time.getEpochSecond - queue.head.time.getEpochSecond > dur.getSeconds) {
            queue.dequeue()
        }
    }

    def updateWriteBefore() {
        writeBefore = lastTime.plus(dur)
    }
}
