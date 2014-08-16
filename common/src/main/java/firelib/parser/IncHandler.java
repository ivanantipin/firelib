package firelib.parser;

import java.nio.CharBuffer;


public class IncHandler<T> extends BaseHandler<T> {
    @Override
    public boolean handle(CharBuffer buffer, T md) {
        if(buffer.position() < buffer.limit()){
            buffer.get();
            return true;
        }
        return false;
    }
}
