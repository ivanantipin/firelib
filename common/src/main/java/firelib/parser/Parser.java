package firelib.parser;

import firelib.common.ISimpleReader;
import firelib.domain.Timed;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
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

    private T currentRecord;
    //21.05.2007,094800,90.05,90.05,90.05,90.05,900,1100
    private final CharBuffer charBuffer;

    long endReadPosition = 0;
    private final CharsetDecoder charsetDecoder;
    private String fileName;


    public Parser(String fileName, IHandler<T>[] handlers, Supplier<T> factory) {
        this(fileName, handlers, factory, 20000000);
    }

    public Parser(String fileName, IHandler<T>[] handlers, Supplier<T> factory, int capacityBytes) {
        this.fileName = fileName;
        try {
            this.capacity = capacityBytes;
            charBuffer = CharBuffer.allocate(capacityBytes*2);
            this.handlers = handlers;
            this.factory = factory;
            File aFile = new File(fileName);
            fileChannel = new RandomAccessFile(aFile, "r").getChannel();
            charSet = Charset.forName("US-ASCII");
            charsetDecoder = charSet.newDecoder();
            initStartEndTimes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private long buffer(long pos, int capacity) {
        MappedByteBuffer mem = null;
        try {
            long len = Math.min(fileChannel.size() - pos, capacity);
            if (len == 0) {
                return len;
            }
            mem = fileChannel.map(FileChannel.MapMode.READ_ONLY, pos, len);
            charsetDecoder.decode(mem, charBuffer, false);
            charBuffer.flip();
            return len;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void initStartEndTimes() throws IOException {
        buffer(0,1000);
        T first = readFromBuffer();
        startDt = first.DtGmt();
        charBuffer.clear();
        long lastPos = fileChannel.size() - 400;
        if (lastPos < 0) {
            buffer(0,1000);
        } else {
            buffer(lastPos,1000);
            if (!align(charBuffer)) {
                throw new RuntimeException("cant read end");
            }
        }

        T last = readFromBuffer();
        while (last != null) {
            endDt = last.DtGmt();
            last = readFromBuffer();
        }
        charBuffer.limit(0);
    }


    boolean align(CharBuffer charBuffer) {
        while (charBuffer.position() < charBuffer.limit() && charBuffer.get() != '\n') {
        }
        while (charBuffer.position() < charBuffer.limit() && BaseHandler.eolOrEmpty(charBuffer.charAt(0))) {
            charBuffer.get();
        }
        return charBuffer.position() < charBuffer.limit();
    }

    @Override
    public boolean seek(Instant time) {
        while (read()) {
            if (time.compareTo(current().DtGmt()) <= 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public T current() {
        return currentRecord;
    }

    @Override
    public boolean read() {
        currentRecord = readFromBuffer();
        if (currentRecord == null) {
            charBuffer.compact();
            System.out.println("buffering " + fileName);
            long len = buffer(endReadPosition,capacity);
            endReadPosition += len;
            if (len == 0) {
                return false;
            }
            int pos = BaseHandler.skippEolOrEmpty(charBuffer,charBuffer.position());
            charBuffer.position(Math.min(pos,charBuffer.limit()));
            currentRecord = readFromBuffer();
        }
        return currentRecord != null;
    }

    private T readFromBuffer() {
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

    @Override
    public void close() throws Exception {
        try {
            fileChannel.close();
        } catch (IOException e) {

        }
    }
}
