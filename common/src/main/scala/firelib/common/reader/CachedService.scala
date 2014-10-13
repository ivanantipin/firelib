package firelib.common.reader

import java.nio.file.{Path, Paths}
import java.time.Instant

import firelib.common.misc.dateUtils
import firelib.domain.Timed


class CachedService(val cacheDirectory : String){

    private val rootDir: Path = Paths.get(cacheDirectory)

    private def makeKey(fn : String, startTime : Instant, endTime : Instant ): String ={

        val st = dateUtils.toStandardString(startTime)
        val et = dateUtils.toStandardString(endTime)
        val path: Path = Paths.get(fn).toAbsolutePath
        s"${path.getParent.getFileName}_${path.getFileName}_$st-$et"
    }




    def checkPresent[T <: Timed](fn : String, startTime : Instant, endTime : Instant, desc: BinaryReaderRecordDescriptor[T]): Option[SimpleReader[T]] ={
        val key = makeKey(fn,startTime,endTime)

        if(!rootDir.resolve(key).toFile.exists()){
            None
        }else{
            Option(new BinaryReader[T](rootDir.resolve(key).toAbsolutePath.toString,desc))
        }
    }



    def write[T <: Timed](fn : String, reader : SimpleReader[T], tt : BinaryReaderRecordDescriptor[T]) : SimpleReader[T] = {
        val key: String = makeKey(fn,reader.startTime(),reader.endTime())
        val cachedFile: String = rootDir.resolve(key).toAbsolutePath.toString
        System.out.println(s"caching $cachedFile")
        val writer: BinaryWriter[T] = new BinaryWriter[T](cachedFile,tt)
        while(reader.read()){
            writer.write(reader.current)
        }
        writer.flush()
        checkPresent(fn,reader.startTime(),reader.endTime(),tt).get
    }

}
