package firelib.common

import java.time.Instant

/**
 * Created by ivan on 7/21/14.
 */
trait ISimpleReader[T <: Timed] {
    def Seek(time:Instant) : Boolean

    def Dispose()

    def UpdateTimeZoneOffset()

    def CurrentQuote: T

    def Read: Boolean

    def StartTime() : Instant
    def EndTime() : Instant


}
