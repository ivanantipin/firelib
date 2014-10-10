package firelib.common.misc

import java.time._
import java.time.format.DateTimeFormatter

/**

 */
object dateUtils {

    val dateStringFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    val nyZoneId = ZoneId.of("America/New_York")
    val londonZoneId = ZoneId.of("Europe/London")
    val moscowZoneId = ZoneId.of("Europe/Moscow")



    implicit class parseStandard(str : String ) {
        def parseStandard : Instant  = LocalDateTime.parse(str,dateStringFormat).toInstant(ZoneOffset.UTC)
    }

    def parseAtZone(str : String, zone : ZoneId) : Instant = {
        LocalDateTime.parse(str,dateStringFormat).atZone(zone).toInstant
    }

    def toStandardString(that : Instant) : String = dateStringFormat.format(that.atZone(ZoneOffset.UTC))


    implicit class instantUtils(that : Instant) {
        def toNyTime : ZonedDateTime = that.atZone(nyZoneId)
        def toLondonTime : ZonedDateTime = that.atZone(londonZoneId)
        def toMoscowTime : ZonedDateTime = that.atZone(moscowZoneId)

        def toStandardString : String = dateStringFormat.format(that.atZone(ZoneOffset.UTC))
    }

}
