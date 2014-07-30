package firelib.robot

import java.time.Instant
import java.util.concurrent.{CopyOnWriteArrayList, Executors, TimeUnit}

import firelib.common._

import scala.collection.JavaConversions._

class Frequencer(val interval: Interval, val precisionMs: Long = 100) extends Runnable {

    private val timeListeners = new CopyOnWriteArrayList[Instant => Unit]()

    private var lastTimeTrigger: Long = _

    val executor = Executors.newSingleThreadScheduledExecutor()

    def addListener(act: Instant => Unit) = {
        timeListeners += act
    }

    def Start() = {
        executor.scheduleAtFixedRate(this, precisionMs, precisionMs, TimeUnit.MILLISECONDS)
    }

    private def notifyListeners(ctime: Instant) = timeListeners.foreach(_(ctime))

    override def run(): Unit = {
        val epochTick = System.currentTimeMillis();
        val rounded = (epochTick / interval.durationMs) * interval.durationMs;
        if (lastTimeTrigger != rounded) {
            lastTimeTrigger = rounded;
            notifyListeners(Instant.ofEpochMilli(rounded));
        }
    }
}
