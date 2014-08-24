package firelib.report

import java.nio.file._

import firelib.common._
import firelib.utils.{JacksonWrapper, statFileDumper}
import org.apache.commons.io.FileUtils

import scala.collection.immutable.HashMap


object reportWriter {
    def write(model: IModel, cfg: ModelConfig, targetDir: String) : Unit = {

        FileUtils.deleteDirectory(Paths.get(targetDir).toFile)

        FileUtils.forceMkdir(Paths.get(targetDir).toFile)

        JacksonWrapper.serialize(cfg,Paths.get(targetDir, "cfg.json").toString)

        var trades = model.trades

        if (trades.length == 0) return

        statFileDumper.writeRows(Paths.get(targetDir, "modelProps.properties").toAbsolutePath.toString,model.properties.map(a=>a._1 + "=" + a._2))


        var factors = if (trades(0).factors == null) new HashMap[String, String] else trades(0).factors

        TradesCsvWriter.write(model, Paths.get(targetDir, "trades.csv").toAbsolutePath.toString, factors.map(_._1))

        copyJarFileToReal("/StdReport.ipynb", Paths.get(targetDir,"StdReport.ipynb").toAbsolutePath.toString)
        copyJarFileToReal("/TradesReporter.py", Paths.get(targetDir,"TradesReporter.py").toAbsolutePath.toString)

        //val envProps  = List("report.lib.path=" + Paths.get(baseDir, "python/report/").toAbsolutePath.toString)
        //StatFileDumper.writeRows(Paths.get(targetDir, "reportenv.properties").toAbsolutePath.toString,  envProps)
    }

    def copyJarFileToReal(jarFile : String, dest : String) : Unit = {
        val inputUrl = getClass().getResourceAsStream(jarFile);
        Files.copy(inputUrl,Paths.get(dest),StandardCopyOption.REPLACE_EXISTING)
    }

}
