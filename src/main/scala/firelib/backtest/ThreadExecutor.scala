package firelib.backtest

import java.util.concurrent._


class ThreadExecutor(val threadsNumber: Int = 1, val maxLengthOfQueue: Int = -1, val threadName: String = "pipeline_") extends IThreadExecutor with ThreadFactory {

    val executor = new ThreadPoolExecutor(threadsNumber, threadsNumber, 1, TimeUnit.SECONDS, new ArrayBlockingQueue[Runnable](maxLengthOfQueue), this)

    //private readonly Logger log;
    //log = LogManager.GetLogger(threadName ?? "pipeline");

    var threadcounter = 0;

    def Execute(act: Unit => Unit) = {
        executor.execute(new Runnable {
            override def run = {
                try {
                    act()
                } catch {
                    case e: Exception => {
                        //TODO log error
                    }

                }
            }
        })
    }

    def Start() {}

    def Stop() {
        executor.shutdown()
        executor.awaitTermination(-1, TimeUnit.HOURS)
    }


    override def newThread(r: Runnable): Thread = {
        val ret: Thread = Executors.defaultThreadFactory().newThread(r)
        ret.setName(threadName + (threadcounter += 1))
        return ret
    }
}
