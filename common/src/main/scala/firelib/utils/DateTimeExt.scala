package firelib.utils



import java.time._
import java.time.format.DateTimeFormatter



object DateTimeExt {

    val dateStringFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    val nyZoneId = ZoneId.of("America/New_York")
    val londonZoneId = ZoneId.of("Europe/London")

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

}
