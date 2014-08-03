package firelib.utils

import java.nio.file._

import firelib.common._

import scala.collection.immutable.HashMap


object ReportWriter {
    def write(model: IModel, cfg: ModelConfig, targetDir: String) : Unit = {

        //FIXME JsonSerializerUtil.Save(cfg, Path.Combine(targetDir, "cfg.json"));

        var trades = model.trades;

        if (trades.length == 0) {
            return;
        }

        val targetFile = Paths.get(targetDir, "modelProps.properties")

        for (kvp <- model.properties) {
            StatFileDumper.AppendRow(targetFile.toFile.getName, kvp._1 + "=" + kvp._2)
        }

        var factors = if (trades(0).Factors == null) new HashMap[String, String] else trades(0).Factors

        TradesCsvWriter.write(model, Paths.get(targetDir, "trades.csv").toFile.getAbsolutePath, factors.map(_._1));

        var baseDir = "./tmp/reportDir";

        val ipypath = Paths.get(baseDir, "Python/IPythonReport/StdReport.ipynb").getFileName;

        Files.copy(ipypath, Paths.get(targetDir, "StdReport.ipynb"), StandardCopyOption.REPLACE_EXISTING)

        StatFileDumper.AppendRow(Paths.get(targetDir, "reportenv.properties").toFile.getAbsolutePath, "report.lib.path=" + Paths.get(baseDir, "Python/ReportLib").getFileName)

    }

}

