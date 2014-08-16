package firelib.common

import java.time.Instant

import firelib.domain.Timed

trait ISimpleReader[+T <: Timed] extends AutoCloseable{

    def seek(time:Instant) : Boolean

    def current: T

    def read(): Boolean

    def startTime() : Instant

    def endTime() : Instant
}
