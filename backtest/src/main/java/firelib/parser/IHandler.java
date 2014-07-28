package firelib.parser;

import java.nio.CharBuffer;


interface IHandler<T> {
    boolean handle(CharBuffer buffer, T md);
}
