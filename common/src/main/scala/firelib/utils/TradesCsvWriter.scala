package firelib.utils

import firelib.common._
import firelib.utils.DateTimeExt._

import scala.collection.mutable.ArrayBuffer

object TradesCsvWriter {

    val decPlaces: Int = 5

    def write(model: IModel, file: String, factorcols: Iterable[String]): Unit = {
        val tradingCases = Utils.toTradingCases(model.trades)
        var rows = new ArrayBuffer[String]()
        rows += (List("Ticker", "BuySell", "EntryDate", "EntryPrice", "ExitDate", "ExitPrice", "Pnl", "nContracts", "MAE", "MFE") ++ factorcols).mkString(";")
        for (t <- tradingCases) {

            val vals = List(
                t._1.security,
                if (t._1.side == Side.Buy) 1 else -1,
                t._1.dtGmt.toStandardString,
                Utils.dbl2Str(t._1.price, decPlaces),
                t._2.dtGmt.toStandardString,
                Utils.dbl2Str(t._2.price, decPlaces),
                Utils.dbl2Str(Utils.pnlForCase(t), decPlaces),
                t._1.qty,
                Utils.dbl2Str(t._2.MAE, decPlaces),
                Utils.dbl2Str(t._2.MFE, decPlaces)
            ) ++ factorcols.map(t._1.factors(_))
            rows += vals.mkString(";")
        }
        StatFileDumper.writeRows(file, rows)
    }
}
