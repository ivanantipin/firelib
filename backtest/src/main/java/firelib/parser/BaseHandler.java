package firelib.parser;

import java.nio.CharBuffer;


abstract class BaseHandler<T> implements IHandler<T> {

    static final boolean sep(char c) {
        return ((c == '\n') || (c == '\r') || (c == '\t') || (c == ' ') || (c == ','));
    }

    static final boolean eol(char c) {
        return ((c == '\n') || (c == '\r'));
    }

    protected int skippTillSep(CharBuffer buffer, int i) {
        while (i < buffer.limit() && !sep(buffer.get(i))) {
            i++;
        }
        return i;
    }

    protected int skippTillEol(CharBuffer buffer, int i) {
        while (i < buffer.limit() && !eol(buffer.get(i))) {
            i++;
        }
        return i;
    }

    protected int skippEol(CharBuffer buffer, int i) {
        while (i < buffer.limit() && eol(buffer.get(i))) {
            i++;
        }
        return i;
    }

    protected int skippTillChar(CharBuffer buffer, int i, char ch) {
        while (i < buffer.limit() && buffer.get(i) != ch) {
            i++;
        }
        return i;
    }
}
