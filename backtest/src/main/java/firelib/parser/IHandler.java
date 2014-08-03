package firelib.parser;

import java.nio.CharBuffer;


public interface IHandler<T> {
    boolean handle(CharBuffer buffer, T md);
}
