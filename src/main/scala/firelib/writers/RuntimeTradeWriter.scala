package firelib.util

import com.firelib.util.Utils
import firelib.domain.Trade

object RuntimeTradeWriter {

    def SerializeTrade(model: String, trade: Trade): String = {
        var b = new StringBuilder();
        b.append(model);
        b.append(";");
        b.append(trade.Security);
        b.append(";");
        b.append(trade.SrcOrder.Id);
        b.append(";");
        b.append(DateTimeExt.ToStandardString(trade.DtGmt));
        b.append(";");
        b.append(trade.TradeSide);
        b.append(";");
        b.append(trade.Qty);
        b.append(";");
        b.append(Utils.Dbl2Str(trade.Price, 2));
        return b.toString();
    }

    def write(fileName: String, model: String, trade: Trade) = {
        StatFileDumper.AppendRow(fileName, SerializeTrade(model, trade))
    }
}

