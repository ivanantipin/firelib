package firelib.util

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

/**
 * Created by ivan on 7/19/14.
 */
object DateTimeExt {

    val dateStringFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    def ToStandardString(dt : DateTime) : String =
    {
        return dt.formatted("yyyy-MM-dd HH:mm:ss")
    }

    def ParseStandard(str : String ) : DateTime =
    {
        return dateStringFormat.parseDateTime(str);
    }


}
