package firelib.common

import java.time.Instant

/**
 * Created by ivan on 7/21/14.
 */
trait ISimpleReader[T <: Timed] {
    def seek(time:Instant) : Boolean

    def Dispose()

    def CurrentQuote: T

    def Read(): Boolean

    def StartTime() : Instant
    def EndTime() : Instant


}
