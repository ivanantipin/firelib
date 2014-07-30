package firelib.parser;

import firelib.common.ISimpleReader;
import firelib.common.Timed;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.function.Supplier;


public class Parser<T extends Timed> implements ISimpleReader<T> {

    private final IHandler[] handlers;
    private final Supplier<T> factory;
    private final int capacity = 100000;
    private final FileChannel fileChannel;

    private final Charset chset;
    private Instant startDt;
    private Instant endDt;

    private T currentQuote;


    public Parser(String fileName, IHandler<T>[] handlers, Supplier<T> factory) {
        try {
            this.handlers = handlers;
            this.factory = factory;
            File aFile = new File(fileName);
            fileChannel = new RandomAccessFile(aFile, "r").getChannel();
            chset = Charset.forName("US-ASCII");
            initFirstAndLast();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private CharBuffer buffer(long pos){
        MappedByteBuffer mem = null;
        try {
            long len = Math.min(fileChannel.size() - pos, capacity);
            mem = fileChannel.map(FileChannel.MapMode.READ_ONLY, pos, len);
            return chset.decode(mem);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initFirstAndLast() throws IOException {
        CharBuffer buffer = buffer(0);
        T first = read(buffer);
        startDt = first.DtGmt();


        long lastPos = fileChannel.size() - 400;
        if(lastPos < 0){
            buffer = buffer(0);
        }else{
            buffer = buffer(lastPos);
            if(!align(buffer)){
                throw new RuntimeException("cant read end");
            }
        }

        T last = read(buffer);
        while(last != null){
            endDt = last.DtGmt();
            last = read(buffer);
        }
    }



    boolean align(CharBuffer charBuffer){
        while (charBuffer.position() < charBuffer.limit() && charBuffer.get() != '\n') {}
        while (charBuffer.position() < charBuffer.limit() && BaseHandler.sep(charBuffer.charAt(0))) {charBuffer.get();}
        return charBuffer.position() < charBuffer.limit();
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

    @Override
    public boolean Seek(Instant time) {

        /*Ohlc curr = next();
        while(curr.DtGmt().isBefore(time)){
            charBuffer.position(charBuffer.position() + 1000000);
            align();
            curr = next();
        }
        charBuffer.position(charBuffer.position() - 1000000);

        while(curr.DtGmt().isBefore(time)){
            curr = next();
        }*/
        return false;

    }

    @Override
    public void Dispose() {

    }

    @Override
    public void UpdateTimeZoneOffset() {

    }

    @Override
    public T CurrentQuote() {
        return currentQuote;
    }

    @Override
    public boolean Read() {
        currentQuote = factory.get();
        for (int i = 0; i < handlers.length; i++) {
        }
        return true;
    }

    private  T read(CharBuffer buffer) {
        T q = factory.get();
        for (int i = 0; i < handlers.length; i++) {
            if (!handlers[i].handle(buffer, q)) {
                return null;
            }
        }
        return q;
    }


    @Override
    public Instant StartTime() {
        return startDt;
    }

    @Override
    public Instant EndTime() {
        return endDt;
    }
}
