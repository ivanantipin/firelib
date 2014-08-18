package firelib.report

import java.nio.file.{Files, Paths, StandardCopyOption}
import java.time.Instant

import firelib.common._
import firelib.utils.StatFileDumper

import scala.collection.immutable.StringOps
import scala.collection.mutable.ArrayBuffer

object OptParamsWriter {

    val decPlaces = 5
    val separator: StringOps = ";"
    def write(targetDir: String, optEnd: Instant, estimates: Seq[ExecutionEstimates], optParams: Seq[OptimizedParameter], metrics: Seq[StrategyMetric]) = {
        var rows = new ArrayBuffer[String]()

        rows += (optParams.map(_.name) ++ metrics.map(_.name)).mkString(separator)
        for (est <- estimates) {
            val opts: Seq[String] = optParams.map(nm => est.optParams(nm.name).toString)
            val calcMetrics: Seq[String] = metrics.map(m => {
                utils.dbl2Str(est.metricToValue(m), decPlaces)
            })
            rows += (opts ++ calcMetrics).mkString(separator)
        }
        StatFileDumper.writeRows(Paths.get(targetDir, "Opt.csv").toString, rows)

        var baseDir = "."

        val ipypath = Paths.get(baseDir, "python/report/ipython/OptStdReport.ipynb")

        Files.copy(ipypath, Paths.get(targetDir, "OptStdReport.ipynb"), StandardCopyOption.REPLACE_EXISTING)

    }
}
