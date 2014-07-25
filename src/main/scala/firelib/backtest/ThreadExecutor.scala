package firelib.backtest

import java.util.concurrent._

import org.slf4j.LoggerFactory


class ThreadExecutor(val threadsNumber: Int = 1, val maxLengthOfQueue: Int = -1, var threadName: String = "pipeline_") extends IThreadExecutor with ThreadFactory {

    val executor = new ThreadPoolExecutor(threadsNumber, threadsNumber, 1, TimeUnit.SECONDS, new ArrayBlockingQueue[Runnable](maxLengthOfQueue), this)


    val log = LoggerFactory.getLogger(threadName)

    var threadcounter = 0;

    def Execute(act: => Unit) = {
        executor.execute(new Runnable {
            override def run = {
                try {
                    act
                } catch {
                    case e: _ => {
                        log.error("exception in pipeline ",e)
                    }

                }
            }
        })
    }

    def Start() : IThreadExecutor ={
        return this
    }

    def Stop() = {
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)
    }


    override def newThread(r: Runnable): Thread = {
        val ret: Thread = Executors.defaultThreadFactory().newThread(r)
        threadcounter += 1
        ret.setName(threadName + threadcounter)
        return ret
    }
}
