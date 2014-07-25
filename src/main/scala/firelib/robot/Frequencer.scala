package firelib.robot

import java.util.concurrent.{CopyOnWriteArrayList, Executors, TimeUnit}

import firelib.domain.Interval
import org.joda.time.DateTime

import scala.collection.JavaConversions._

class Frequencer(val interval: Interval, val precisionMs: Long = 100) extends Runnable {

    private val timeListeners = new CopyOnWriteArrayList[DateTime => Unit]()

    private var lastTimeTrigger: Long = _

    val executor = Executors.newSingleThreadScheduledExecutor()

    def AddListener(act: DateTime => Unit) = {
        timeListeners += act
    }

    def Start() = {
        executor.scheduleAtFixedRate(this, precisionMs, precisionMs, TimeUnit.MILLISECONDS)
    }

    private def NotifyListeners(ctime: DateTime) = for (tl <- timeListeners) tl(ctime)

    override def run(): Unit = {
        val epochTick = System.currentTimeMillis();
        val rounded = (epochTick / interval.durationMs) * interval.durationMs;
        if (lastTimeTrigger != rounded) {
            lastTimeTrigger = rounded;
            NotifyListeners(new DateTime(rounded));
        }
    }
}
