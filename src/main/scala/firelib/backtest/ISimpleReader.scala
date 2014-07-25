package firelib.backtest

import firelib.domain.Timed
import org.joda.time.DateTime

/**
 * Created by ivan on 7/21/14.
 */
trait ISimpleReader[T <: Timed] {
    def Seek(time: DateTime) : Boolean

    def Dispose()

    def UpdateTimeZoneOffset()

    def CurrentQuote: T

    def Read: Boolean

}
