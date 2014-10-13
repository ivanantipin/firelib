package firelib.common.reader

import java.nio.ByteBuffer
import java.time.Instant

import firelib.domain.{Ohlc, Tick}

class TickDesc extends BinaryReaderRecordDescriptor[Tick]{
    override def write(tick: Tick, buffer: ByteBuffer): Unit = {
        buffer.putDouble(tick.getLast)
        buffer.putInt(tick.getVol)
        buffer.putDouble(tick.getBid)
        buffer.putDouble(tick.getAsk)
        buffer.putLong(tick.getDtGmt.toEpochMilli)
        buffer.putInt(tick.getTickNumber)
    }

    def sample : Tick = new Tick()

    override def read(buff: ByteBuffer): Tick = {
        val tick = new Tick()
        tick.last = buff.getDouble()
        tick.vol = buff.getInt
        tick.bid = buff.getDouble()
        tick.ask = buff.getDouble()
        tick.dtGmt = Instant.ofEpochMilli(buff.getLong())
        tick.tickNumber = buff.getInt()
        tick
    }
}

class OhlcDesc extends BinaryReaderRecordDescriptor[Ohlc]{
    override def write(tick: Ohlc, buffer: ByteBuffer): Unit = {
        buffer.putDouble(tick.getO)
        buffer.putDouble(tick.getH)
        buffer.putDouble(tick.getL)
        buffer.putDouble(tick.getC)
        buffer.putInt(tick.getVolume)
    }

    def sample : Ohlc = new Ohlc()

    override def read(buff: ByteBuffer): Ohlc = {
        val ohlc = new Ohlc()
        ohlc.O = buff.getDouble()
        ohlc.H = buff.getDouble()
        ohlc.L = buff.getDouble()
        ohlc.C = buff.getDouble()
        ohlc.Volume = buff.getInt()
        ohlc
    }
}

