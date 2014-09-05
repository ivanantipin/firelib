package firelib.common.report

import firelib.common.misc.{statFileDumper, dateUtils, utils}
import firelib.common.model.Model
import firelib.common.Side
import dateUtils._

import scala.collection.mutable.ArrayBuffer

/**
 * Created by ivan on 9/5/14.
 */
object TradesCsvWriter {

    val decPlaces: Int = 5

    def write(model: Model, file: String, factorcols: Iterable[String]): Unit = {
        val tradingCases = utils.toTradingCases(model.trades)
        var rows = new ArrayBuffer[String]()
        rows += (List("Ticker", "BuySell", "EntryDate", "EntryPrice", "ExitDate", "ExitPrice", "Pnl", "nContracts", "MAE", "MFE") ++ factorcols).mkString(";")
        for (t <- tradingCases) {

            val vals = List(
                t._1.security,
                if (t._1.side == Side.Buy) 1 else -1,
                t._1.dtGmt.toStandardString,
                utils.dbl2Str(t._1.price, decPlaces),
                t._2.dtGmt.toStandardString,
                utils.dbl2Str(t._2.price, decPlaces),
                utils.dbl2Str(utils.pnlForCase(t), decPlaces),
                t._1.qty,
                utils.dbl2Str(t._2.MAE, decPlaces),
                utils.dbl2Str(t._2.MFE, decPlaces)
            ) ++ factorcols.map(t._1.factors(_))
            rows += vals.mkString(";")
        }
        statFileDumper.writeRows(file, rows)
    }
}
