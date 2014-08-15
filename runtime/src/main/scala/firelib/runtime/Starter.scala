package firelib.execution

import java.io.{File, FileFilter}
import java.net.URL
import java.nio.file.Paths

import firelib.common._
import firelib.robot.{ModelRuntimeConfig, ModelRuntimeContainer}
import firelib.strats.dummy.DummyStrat
import firelib.utils.JacksonWrapper

import scala.collection.mutable.ArrayBuffer
;

object Starter {

    val lst = new ArrayBuffer[ModelRuntimeContainer]()

    val jsonFilter = new FileFilter {
        override def accept(pathname: File): Boolean = pathname.getName.endsWith(".json")
    }

    def start(confDir : String) {

        for(f <- new File(confDir).listFiles(jsonFilter)){
            val cfg = JacksonWrapper.deserialize(f.toPath.toAbsolutePath.toString, classOf[ModelRuntimeConfig])
            val container: ModelRuntimeContainer = new ModelRuntimeContainer(cfg)
            container.start()
            lst += container
        }
    }

    def main(args : Array[String]) {
        genDummy()
        start("/home/ivan/tmp/")
    }

    def getFile(name: String): String = {
        val resource: URL = this.getClass.getClassLoader.getResource("configs/dummy.txt")
        return Paths.get(resource.getFile).getParent.resolve(name).toAbsolutePath.toString
    }


    def genDummy() = {
        val cfg = new ModelRuntimeConfig()
        cfg.gatewayConfig = Map("port"->"4001","client.id"->"1")
        cfg.gatewayType="IB"
        cfg.runBacktest = false
        cfg.modelConfig.reportRoot = "/home/ivan/tmp"
        cfg.modelConfig.frequencyIntervalId = Interval.Sec10.Name
        cfg.modelConfig.className = classOf[DummyStrat].getName
        cfg.modelConfig.addTickerId(new TickerConfig("EURUSD","",MarketDataType.Ohlc))
        JacksonWrapper.serialize(cfg, "/home/ivan/tmp/dummyModel.json")

    }
}
