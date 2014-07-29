package firelib.parser;

import firelib.common.Ohlc;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.time.Instant;


public class Parser {

    private final FileInputStream inFile;
    private final InputStreamReader inputStreamReader;
    private final IHandler[] handlers;
    private CharBuffer charBuffer = CharBuffer.allocate(10000000);

    public Parser(String fileName, IHandler[] handlers) throws Exception {
        this.handlers = handlers;
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


    //FIXME
    public void seek(Instant time) throws Exception {

        Ohlc curr = next();
        while(curr.DtGmt().isBefore(time)){
            charBuffer.position(charBuffer.position() + 1000000);
            align();
            curr = next();
        }
        charBuffer.position(charBuffer.position() - 1000000);

        while(curr.DtGmt().isBefore(time)){
            curr = next();
        }

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
/*
            Parser parser = new Parser("C:\\usr\\temp\\1_#.csv");
            Ohlc oh = null;
            long start = System.currentTimeMillis();
            int cnt = 0;
            while ((oh = parser.next()) != null) {
                cnt++;
            }
            ;
            System.out.println(cnt / (System.currentTimeMillis() - start));
*/

        }


    }

}
