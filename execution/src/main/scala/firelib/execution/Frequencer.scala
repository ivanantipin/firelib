package firelib.robot

import java.time.Instant
import java.util.concurrent.{Executors, TimeUnit}

import firelib.common._

class Frequencer(val interval: Interval, val listeners : Seq[IStepListener], val callbackExecutor : IThreadExecutor,  val precisionMs: Long = 100) extends Runnable {

    private var lastTimeTrigger: Instant = _

    val executor = Executors.newSingleThreadScheduledExecutor()

    def start() : Unit = {
        executor.scheduleAtFixedRate(this, precisionMs, precisionMs, TimeUnit.MILLISECONDS)
    }

    override def run(): Unit = {
        val rounded = Instant.ofEpochMilli(interval.roundEpochMs(System.currentTimeMillis))
        if (lastTimeTrigger != rounded) {
            lastTimeTrigger = rounded
            callbackExecutor.execute(()=>listeners.foreach(_.onStep(rounded)))
        }
    }
}
