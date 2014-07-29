package firelib.utils

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}


object DateTimeExt {

    val dateStringFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    def ToStandardString(dt :Instant) : String =
    {
        return dateStringFormat.format(dt)
    }

    def ParseStandard(str : String ) : Instant  =
    {
        return LocalDateTime.parse(str,dateStringFormat).toInstant(ZoneOffset.UTC)
    }


}
