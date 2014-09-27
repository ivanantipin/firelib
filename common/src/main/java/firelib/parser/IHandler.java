package firelib.parser;

import java.nio.CharBuffer;


public interface IHandler<T> {
    int handle(CharBuffer buffer, T md);
}
