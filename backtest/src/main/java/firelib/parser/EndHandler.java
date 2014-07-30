package firelib.parser;

import java.nio.CharBuffer;

public class EndHandler extends BaseHandler{
    @Override
    public boolean handle(CharBuffer buffer, Object md) {
        int i = buffer.position();
        i = skippTillEol(buffer, i);
        i = skippEol(buffer, i);
        buffer.position(Math.min(i,buffer.limit()));
        return true;
    }
}
