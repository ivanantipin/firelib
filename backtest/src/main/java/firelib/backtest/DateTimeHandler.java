package firelib.backtest;

import java.nio.CharBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

class DateTimeHandler<T> extends BaseHandler<T> {
    private final IModifier<T, Instant> consumer;
    private ZoneId zoneId;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy,HHmmss.SSS");


    public DateTimeHandler(IModifier<T,Instant> consumer, ZoneId zoneId){

        this.consumer = consumer;
        this.zoneId = zoneId;
    }

    public boolean handle(CharBuffer buffer, T md) {
        int i = buffer.position();
        i = skippTillChar(buffer, i, ',');
        i++;
        i = skippTillSep(buffer, i);

        if (i == buffer.limit()) {
            return false;
        }

        Instant instant = LocalDateTime.parse(buffer.subSequence(0, i - buffer.position()), formatter).atZone(zoneId).toInstant();
        consumer.apply(md,instant);
        i++;
        buffer.position(i);
        return true;
    }

}
