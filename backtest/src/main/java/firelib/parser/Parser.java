package firelib.parser;

import firelib.common.ISimpleReader;
import firelib.common.Tick;
import firelib.common.Timed;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.time.Instant;
import java.util.function.Supplier;


public class Parser<T extends Timed> implements ISimpleReader<T> {

    private final IHandler[] handlers;
    private final Supplier<T> factory;
    private final int capacity;
    private final FileChannel fileChannel;

    private final Charset charSet;
    private Instant startDt;
    private Instant endDt;

    private T currentQuote;
//21.05.2007,094800,90.05,90.05,90.05,90.05,900,1100
    CharBuffer charBuffer = CharBuffer.allocate(40000000);

    long endReadPosition = 0;
    private CharsetDecoder charsetDecoder;


    public Parser(String fileName, IHandler<T>[] handlers, Supplier<T> factory) {
        this(fileName, handlers, factory, 80000000);
    }

    public Parser(String fileName, IHandler<T>[] handlers, Supplier<T> factory, int capacity) {
        try {
            this.capacity = capacity;
            this.handlers = handlers;
            this.factory = factory;
            File aFile = new File(fileName);
            fileChannel = new RandomAccessFile(aFile, "r").getChannel();
            charSet = Charset.forName("US-ASCII");
            charsetDecoder = charSet.newDecoder();
            initFirstAndLast();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }



    private long buffer(long pos){
        MappedByteBuffer mem = null;
        try {
            long len = Math.min(fileChannel.size() - pos, capacity);
            if(len == 0){
                return len;
            }
            mem = fileChannel.map(FileChannel.MapMode.READ_ONLY, pos, len);
            CoderResult result = charsetDecoder.decode(mem, charBuffer, false);
            //System.out.println("len " + result.toString());
            charBuffer.flip();
            return len;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    private void initFirstAndLast() throws IOException {
        buffer(0);
        T first = read();
        startDt = first.DtGmt();
        charBuffer.clear();
        long lastPos = fileChannel.size() - 400;
        if(lastPos < 0){
            buffer(0);
        }else{
            buffer(lastPos);
            if(!align(charBuffer)){
                throw new RuntimeException("cant read end");
            }
        }

        T last = read();
        while(last != null){
            endDt = last.DtGmt();
            last = read();
        }
        charBuffer.limit(0);
    }



    boolean align(CharBuffer charBuffer){
        while (charBuffer.position() < charBuffer.limit() && charBuffer.get() != '\n') {}
        while (charBuffer.position() < charBuffer.limit() && BaseHandler.eolOrEmpty(charBuffer.charAt(0))) {charBuffer.get();}
        return charBuffer.position() < charBuffer.limit();
    }




    public static void main(String[] args) throws Exception {
        TokenGenerator tokenGenerator = new TokenGenerator(new CommonIniSettings().initFromFile("/home/ivan/tmp/testDsRoot/TICKS/common.ini"));
        for (int i = 0; i < 100; i++) {


            Parser parser = new Parser<Tick>("/home/ivan/tmp/testDsRoot/TICKS/XG_#.csv",
                    tokenGenerator.handlers ,()->new Tick());

            long start = System.currentTimeMillis();
            int cnt = 0;
            while (parser.Read()) {
                cnt++;
            }
            System.out.println(cnt / (System.currentTimeMillis() - start));

        }


    }

    @Override
    public boolean seek(Instant time) {
        while (Read()){
            if(time.compareTo(CurrentQuote().DtGmt()) <= 0){
                return true;
            }
        }
        return false;
    }

    @Override
    public void Dispose() {
        try {
            fileChannel.close();
        } catch (IOException e) {

        }
    }

    @Override
    public T CurrentQuote() {
        return currentQuote;
    }

    @Override
    public boolean Read() {
        currentQuote = read();
        if(currentQuote == null){
            charBuffer.compact();
            long len = buffer(endReadPosition);
            endReadPosition += len;
            if(len == 0){
                return false;
            }
            currentQuote = read();
        }
        return currentQuote != null;
    }

    private  T read() {
        T q = factory.get();
        int oldPos = charBuffer.position();
        for (int i = 0; i < handlers.length; i++) {
            if (!handlers[i].handle(charBuffer, q)) {
                charBuffer.position(oldPos);
                return null;
            }
        }
        return q;
    }



    @Override
    public Instant startTime() {
        return startDt;
    }

    @Override
    public Instant endTime() {
        return endDt;
    }
}
