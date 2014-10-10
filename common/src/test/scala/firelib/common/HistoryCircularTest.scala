package firelib.common

import firelib.common.timeseries.HistoryCircular
import org.junit.Assert._
import org.junit.{Assert, Test}

class HistoryCircularTest {


    @Test
    def TestRingBuffer() = {
        val bufferPreInited = new HistoryCircular[Array[Long]](3, () => Array[Long](0))
        var nl = bufferPreInited.shift
        nl(0) = 1
        assertEquals(1L, bufferPreInited(0)(0))
        nl = bufferPreInited.shift
        nl(0) = 2
        Assert.assertEquals(1L, bufferPreInited(-1)(0))
        Assert.assertEquals(2L, bufferPreInited(0)(0))
        nl = bufferPreInited.shift
        nl(0) = 3
        nl = bufferPreInited.shift
        nl(0) = 4
        Assert.assertEquals(2L, bufferPreInited(-2)(0))
        Assert.assertEquals(4L, bufferPreInited(0)(0))
    }

}
