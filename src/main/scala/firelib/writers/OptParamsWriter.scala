﻿package firelib.util

import java.nio.file.{Files, Paths, StandardCopyOption}

import com.firelib.util.Utils
import firelib.backtest.StrategyMetric
import firelib.domain.{ExecutionEstimates, OptimizedParameter}
import org.joda.time.DateTime

import scala.collection.mutable.ArrayBuffer

object OptParamsWriter {

    def write(targetDir: String, OptEnd: DateTime, Estimates: Seq[ExecutionEstimates], optParams: Seq[OptimizedParameter], metrics: Seq[StrategyMetric]) = {
        var rows = new ArrayBuffer[String]()
        rows += (optParams.map(_.Name) ++ ";" ++ metrics.map(_.Name)).mkString(";")
        for (est <- Estimates) {
            val opts: Seq[String] = optParams.map(nm => est.OptParams(nm.Name).toString)
            val calcMetrics: Seq[String] = metrics.map(m => Utils.Dbl2Str(est.MetricName2Value(m),5))
            rows += (opts ++ calcMetrics).mkString(";")
        }
        StatFileDumper.writeRows(Paths.get(targetDir, "Opt.csv").toString, rows);

        val ipypath = Paths.get("/home/ivan/tmp/report", "Python/IPythonReport/OptStdReport.ipynb");

        Files.copy(ipypath, Paths.get(targetDir, "OptStdReport.ipynb"), StandardCopyOption.REPLACE_EXISTING);

    }
}