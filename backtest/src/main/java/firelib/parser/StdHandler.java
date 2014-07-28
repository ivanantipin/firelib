package firelib.parser;

import java.nio.CharBuffer;
import java.util.function.Function;

/**
 * Created by ivan on 7/27/14.
 */
class StdHandler<T,V> extends BaseHandler<T> {

    private IModifier<T, V> consumer;
    private Function<CharSequence, V> parser;

    public StdHandler(IModifier<T, V> consumer, Function<CharSequence, V> parser){
        this.consumer = consumer;
        this.parser = parser;
    }

    @Override
    public boolean handle(CharBuffer buffer, T md) {
        int i = buffer.position();
        i = skippTillSep(buffer, i);
        if (i == buffer.limit()) {
            return false;
        }
        CharBuffer seq = buffer.subSequence(0, i - buffer.position());
        consumer.apply(md, parser.apply(seq));
        i++;
        buffer.position(i);
        return true;
    }

}
