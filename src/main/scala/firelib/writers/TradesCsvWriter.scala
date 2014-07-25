package firelib.writers

import com.firelib.util.Utils
import firelib.backtest.IModel
import firelib.domain.Side
import firelib.util.StatFileDumper

import scala.collection.mutable.ArrayBuffer

object TradesCsvWriter {
    def write(model: IModel, file: String, factorcols: Iterable[String]) = {

        var tradingCases = Utils.GetTradingCases(model.trades);

        var rows = new ArrayBuffer[String]();
        rows += "Ticker;BuySell;EntryDate;EntryPrice;ExitDate;ExitPrice;Pnl;nContracts;MAE;MFE;" + factorcols.mkString(";")

        for (t <- tradingCases) {

            var str = String.format("%s;%s;{2:dd.MM.yyyy HH:mm:ss};%s;{4:dd.MM.yyyy HH:mm:ss};%s;%s;%s;%s;%s",
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
            );
            rows += ";" + factorcols.map(t._1.Factors(_)).mkString(";");
        }
        StatFileDumper.writeRows(file, rows);
    }
}
