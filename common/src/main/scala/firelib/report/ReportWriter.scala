package firelib.report

import java.nio.file._

import firelib.common._
import firelib.utils.{JacksonWrapper, StatFileDumper}

import scala.collection.immutable.HashMap


object ReportWriter {
    def write(model: IModel, cfg: ModelConfig, targetDir: String) : Unit = {

        JacksonWrapper.serialize(cfg,Paths.get(targetDir, "cfg.json").toString)

        var trades = model.trades

        if (trades.length == 0) return

        StatFileDumper.writeRows(Paths.get(targetDir, "modelProps.properties").toAbsolutePath.toString,model.properties.map(a=>a._1 + "=" + a._2))


        var factors = if (trades(0).factors == null) new HashMap[String, String] else trades(0).factors

        TradesCsvWriter.write(model, Paths.get(targetDir, "trades.csv").toAbsolutePath.toString, factors.map(_._1))

        var baseDir = "."

        val ipypath = Paths.get(baseDir, "python/report/ipython/StdReport.ipynb")

        Files.copy(ipypath, Paths.get(targetDir, "StdReport.ipynb"), StandardCopyOption.REPLACE_EXISTING)

        val envProps  = List("report.lib.path=" + Paths.get(baseDir, "python/report/").toAbsolutePath.toString)
        StatFileDumper.writeRows(Paths.get(targetDir, "reportenv.properties").toAbsolutePath.toString,  envProps)

    }

}

