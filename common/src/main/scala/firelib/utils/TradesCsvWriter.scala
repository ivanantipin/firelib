package firelib.utils

import firelib.common._

import scala.collection.mutable.ArrayBuffer

object TradesCsvWriter {

    def write(model: IModel, file: String, factorcols: Iterable[String]) = {
        val tradingCases = Utils.GetTradingCases(model.trades);
        var rows = new ArrayBuffer[String]();
        rows += "Ticker;BuySell;EntryDate;EntryPrice;ExitDate;ExitPrice;Pnl;nContracts;MAE;MFE;" + factorcols.mkString(";")
        for (t <- tradingCases) {
            var str = List(
                t._1.Security,
                if (t._1.TradeSide == Side.Buy) 1 else -1,
                t._1.DtGmt,
                t._1.Price,
                t._2.DtGmt,
                t._2.Price,
                Utils.Dbl2Str(Utils.PnlForCase(t), 5),
                t._1.Qty,
                Utils.Dbl2Str(t._2.MAE, 5),
                Utils.Dbl2Str(t._2.MFE, 5)
            ).mkString(";");
            rows += ";" + factorcols.map(t._1.Factors(_)).mkString(";");
        }
        StatFileDumper.writeRows(file, rows);
    }
}
