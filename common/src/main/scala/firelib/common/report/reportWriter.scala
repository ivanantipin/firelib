package firelib.common.report

import java.nio.file.{Files, Paths, StandardCopyOption}

import firelib.common.config.ModelBacktestConfig
import firelib.common.core.ModelOutput
import firelib.common.misc.{jsonHelper, statFileDumper}
import org.apache.commons.io.FileUtils

import scala.collection.immutable.HashMap


object reportWriter {

    def clearReportDir(targetDir: String) : Unit = {

        FileUtils.deleteDirectory(Paths.get(targetDir).toFile)

        FileUtils.forceMkdir(Paths.get(targetDir).toFile)
    }


    def write(model: ModelOutput, cfg: ModelBacktestConfig, targetDir: String) : Unit = {

        jsonHelper.serialize(cfg,Paths.get(targetDir, "cfg.json"))

        var trades = model.trades

        if (trades.length == 0) return

        statFileDumper.writeRows(Paths.get(targetDir, "modelProps.properties").toAbsolutePath.toString,model.model.properties.map(a=>a._1 + "=" + a._2))

        var factors = if (trades(0).factors == null) new HashMap[String, String] else trades(0).factors



        val tradeWriter : StreamTradeCaseWriter = new StreamTradeCaseWriter(Paths.get(targetDir, "trades.csv").toAbsolutePath, factors.map(_._1))
        tradeWriter.writeHeader()
        model.trades.foreach(tradeWriter)

        val orderWriter = new StreamOrderWriter(Paths.get(targetDir, "orders.csv").toAbsolutePath)
        orderWriter.writeHeader()
        model.orderStates.filter(_.status.isFinal).map(_.order).foreach(orderWriter)

        copyJarFileToReal("/StdReport.ipynb", Paths.get(targetDir,"StdReport.ipynb").toAbsolutePath.toString)
        copyJarFileToReal("/TradesReporter.py", Paths.get(targetDir,"TradesReporter.py").toAbsolutePath.toString)

        System.out.println(s"report written to $targetDir you can run it with command 'ipython notebook StdReport.ipynb'")

        //val envProps  = List("report.lib.path=" + Paths.get(baseDir, "python/report/").toAbsolutePath.toString)
        //StatFileDumper.writeRows(Paths.get(targetDir, "reportenv.properties").toAbsolutePath.toString,  envProps)
    }

    def copyJarFileToReal(jarFile : String, dest : String) : Unit = {
        val inputUrl = getClass().getResourceAsStream(jarFile);
        Files.copy(inputUrl,Paths.get(dest),StandardCopyOption.REPLACE_EXISTING)
    }

}
