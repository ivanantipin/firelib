package firelib.common.misc

import java.time._
import java.time.format.DateTimeFormatter

/**
 * Created by ivan on 9/5/14.
 */
object dateUtils {

    val dateStringFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    val nyZoneId = ZoneId.of("America/New_York")
    val londonZoneId = ZoneId.of("Europe/London")
    val moscowZoneId = ZoneId.of("Europe/Moscow")

    implicit class parseStandard(str : String ) {
        def parseStandard : Instant  = LocalDateTime.parse(str,dateStringFormat).toInstant(ZoneOffset.UTC)
    }

    implicit class toStandardString(that : Instant) {
        def toStandardString : String = dateStringFormat.format(that.atZone(ZoneOffset.UTC))
    }

    implicit class toNyTime(that : Instant) {
        def toNyTime : ZonedDateTime = that.atZone(nyZoneId)
    }

    implicit class toLondonTime(that : Instant) {
        def toLondonTime : ZonedDateTime = that.atZone(londonZoneId)
    }

    implicit class toMoscowTime(that : Instant) {
        def toLondonTime : ZonedDateTime = that.atZone(moscowZoneId)
    }

}
