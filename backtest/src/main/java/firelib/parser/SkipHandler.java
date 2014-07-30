package firelib.parser;

import java.nio.CharBuffer;

/**
 * Created by ivan on 7/28/14.
 */
public class SkipHandler extends BaseHandler{
    @Override
    public boolean handle(CharBuffer buffer, Object md) {
        int i = buffer.position();
        i = skippTillSep(buffer, i);
        if (i == buffer.limit()) {
            return false;
        }
        i++;
        buffer.position(i);
        return true;
    }
}

