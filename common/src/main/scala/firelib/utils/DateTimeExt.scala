package firelib.utils



import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}



object DateTimeExt {

    val dateStringFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    implicit class parseStandard(str : String ) {
        def parseStandard : Instant  = LocalDateTime.parse(str,dateStringFormat).toInstant(ZoneOffset.UTC)

    }

    implicit class toStandardString(that : Instant) {
        def toStandardString : String = dateStringFormat.format(that.atZone(ZoneOffset.UTC))
    }

}
