package firelib.common.reader

import java.time.Instant

import firelib.domain.Timed

/**
 * Created by ivan on 9/5/14.
 */
trait SimpleReader[+T <: Timed] extends AutoCloseable{

    def seek(time:Instant) : Boolean

    def current: T

    def read(): Boolean

    def startTime() : Instant

    def endTime() : Instant
}
