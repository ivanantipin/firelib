package firelib.parser;

import java.nio.CharBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@FunctionalInterface
interface Skipper{
    int skip(CharBuffer buffer, int i);
}

class DateTimeHandler<T> extends BaseHandler<T> {
    private final IModifier<T, Instant> consumer;
    private ZoneId zoneId;

    DateTimeFormatter formatter;

    Skipper skipper;

    public DateTimeHandler(IModifier<T,Instant> consumer, ZoneId zoneId, String dateFormat){

        this.consumer = consumer;
        this.zoneId = zoneId;
        formatter = DateTimeFormatter.ofPattern(dateFormat);
        if(dateFormat.indexOf(sep) == -1){
            skipper = this::skipOne;
        }else{
            skipper = this::skipTwo;
        }
    }

    public boolean handle(CharBuffer buffer, T md) {
        int i = buffer.position();
        i = skipper.skip(buffer, i);
        if (i >= buffer.limit()) {
            return false;
        }

        Instant instant = LocalDateTime.parse(buffer.subSequence(0, i - buffer.position()), formatter).atZone(zoneId).toInstant();
        consumer.apply(md,instant);
        buffer.position(i);
        return true;
    }

    private int skipTwo(CharBuffer buffer, int i) {
        i = skippTillChar(buffer, i, sep);
        i++;
        i = skipTillEolOrSep(buffer, i);
        return i;
    }

    private int skipOne(CharBuffer buffer, int i) {
        return skipTillEolOrSep(buffer, i);
    }


}
