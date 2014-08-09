package firelib.utils



import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}



object DateTimeExt {

    val dateStringFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    implicit class ToDtGm(str : String ) {
        def toDtGmt : Instant  = LocalDateTime.parse(str,dateStringFormat).toInstant(ZoneOffset.UTC)

    }

    implicit class ToStandardString(that : Instant) {
        def toStandardString = dateStringFormat.format(that.atZone(ZoneOffset.UTC))
    }

}
