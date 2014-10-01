package firelib.execution

import java.time.Instant
import java.util.concurrent.{Executors, TimeUnit}

import firelib.common.interval.Interval
import firelib.common.threading.ThreadExecutor

class Frequencer(val interval: Interval, val listener : Instant=>Unit, val callbackExecutor : ThreadExecutor,  val precisionMs: Long = 100) extends Runnable {

    private var lastTimeTrigger: Instant = _

    val executor = Executors.newSingleThreadScheduledExecutor()

    def start() : Unit = {
        executor.scheduleAtFixedRate(this, precisionMs, precisionMs, TimeUnit.MILLISECONDS)
    }

    override def run(): Unit = {
        val rounded = Instant.ofEpochMilli(interval.roundEpochMs(System.currentTimeMillis))
        if (lastTimeTrigger != rounded) {
            lastTimeTrigger = rounded
            callbackExecutor.execute(()=>listener(rounded))
        }
    }
}
