package firelib.common.report

import java.nio.file.Paths
import java.time.Instant

import firelib.common.misc.{statFileDumper, utils}
import firelib.common.opt.OptimizedParameter

import scala.collection.immutable.StringOps
import scala.collection.mutable.ArrayBuffer

/**
 * Created by ivan on 9/5/14.
 */
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
        statFileDumper.writeRows(Paths.get(targetDir, "Opt.csv").toString, rows)
    }
}
