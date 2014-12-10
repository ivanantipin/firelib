package firelib.common.report

import java.io.{BufferedOutputStream, OutputStreamWriter}
import java.nio.file.StandardOpenOption._
import java.nio.file.{Files, Path}

import firelib.common.Trade
import firelib.common.misc.StreamTradeCaseGenerator

class StreamTradeCaseWriter(val path : Path, val factors : Iterable[String]) extends TradeSerializer with (Trade=>Unit){

    val stream =  new OutputStreamWriter(new BufferedOutputStream(Files.newOutputStream(path, CREATE, APPEND)))

    val generator = new StreamTradeCaseGenerator()

    def writeHeader(): Unit = {
        stream.write((getHeader() ++ factors).mkString(separator) :+ '\n')
    }

    override def apply(trade : Trade): Unit = {
        for(c <- generator(trade)){
            stream.write((serialize(c) ++ factors.map(c._1.factors(_))).mkString(separator) :+ '\n')
        }
        stream.flush()
    }
}
