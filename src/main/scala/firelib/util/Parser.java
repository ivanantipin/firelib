package firelib.util;

import javolution.text.TypeFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


class Ohlc {
    public double Close;
    public LocalDateTime dateTime;

    @Override
    public String toString() {
        return "Ohlc{" +
                "Close=" + Close +
                ", dateTime=" + dateTime +
                '}';
    }
}

interface IHandler {
    boolean handle(CharBuffer buffer, Ohlc ohlc);
}


abstract class BaseHandler implements IHandler {

    static final boolean sep(char c) {
        return ((c == '\n') || (c == '\r') || (c == '\t') || (c == ' ') || (c == ','));
    }

    protected int skippTillSep(CharBuffer buffer, int i) {
        while (i < buffer.limit() && !sep(buffer.get(i))) {
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

class HHandler extends BaseHandler {

    @Override
    public boolean handle(CharBuffer buffer, Ohlc ohlc) {
        //System.out.println("hh");
        int i = buffer.position();
        i = skippTillSep(buffer, i);
        if (i == buffer.limit()) {
            return false;
        }
        ohlc.Close = TypeFormat.parseFloat(buffer.subSequence(0, i - buffer.position()));
        i++;
        buffer.position(i);
        return true;
    }
}

class Skipper extends BaseHandler {

    @Override
    public boolean handle(CharBuffer buffer, Ohlc ohlc) {
        //System.out.println("ss");
        while (sep(buffer.charAt(0))) {
        }
        return true;
    }
}


class DateTimeHandler extends BaseHandler {

    //08.03.2013,000037.500
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy,HHmmss.SSS");

    public boolean handle(CharBuffer buffer, Ohlc ohlc) {
        //System.out.println("dt");
        int i = buffer.position();
        i = skippTillChar(buffer, i, ',');
        i++;
        i = skippTillSep(buffer, i);

        if (i == buffer.limit()) {
            return false;
        }

        ohlc.dateTime = LocalDateTime.parse(buffer.subSequence(0, i - buffer.position()), formatter);
        i++;
        buffer.position(i);
        return true;
    }

}


public class Parser {

    private final FileInputStream inFile;
    private final InputStreamReader inputStreamReader;
    private CharBuffer charBuffer = CharBuffer.allocate(10000000);

    IHandler[] handlers = new IHandler[]{new DateTimeHandler(), new HHandler(), new HHandler(), new HHandler(), new HHandler(), new HHandler(), new Skipper()};

    public Parser(String fileName) throws Exception {
        File aFile = new File(fileName);
        inFile = new FileInputStream(aFile);
        inputStreamReader = new InputStreamReader(inFile);
        charBuffer.flip();
        bu();
    }

    private boolean bu() throws Exception {
        charBuffer.compact();
        int read = inputStreamReader.read(charBuffer);
        charBuffer.flip();
        return read > 0;
    }

    boolean align(){
        while (charBuffer.position() < charBuffer.limit() && charBuffer.get() != '\n') {}
        while (charBuffer.position() < charBuffer.limit() && BaseHandler.sep(charBuffer.charAt(0))) {charBuffer.get();}
        return charBuffer.position() < charBuffer.limit();
    }


    public void seek(LocalDateTime time) throws Exception {

        align();
        Ohlc first = next();


    }

    public Ohlc next() throws Exception {
        Ohlc ret = new Ohlc();
        for (int i = 0; i < handlers.length; i++) {
            if (!handlers[i].handle(charBuffer, ret)) {
                charBuffer.compact();
                if (bu()) {
                    i--;
                } else {
                    return null;
                }
            }
        }
        return ret;
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10; i++) {
            Parser parser = new Parser("C:\\usr\\temp\\1_#.csv");
            Ohlc oh = null;
            long start = System.currentTimeMillis();
            int cnt = 0;
            while ((oh = parser.next()) != null) {
                cnt++;
            }
            ;
            System.out.println(cnt / (System.currentTimeMillis() - start));

        }


    }

}