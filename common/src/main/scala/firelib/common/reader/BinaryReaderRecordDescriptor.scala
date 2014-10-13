package firelib.common.reader

import java.nio.ByteBuffer

trait BinaryReaderRecordDescriptor[T]{
    def write(t : T, bb : ByteBuffer)
    def read (bb : ByteBuffer) : T
    def sample : T
}
