package firelib.report

import firelib.common._
import firelib.utils.DateTimeExt._
import firelib.utils.statFileDumper

object RuntimeTradeWriter {

    def SerializeTrade(model: String, trade: Trade): String = {
        var b = new StringBuilder()
        b.append(model)
        b.append("")
        b.append(trade.security)
        b.append("")
        b.append(trade.order.id)
        b.append("")
        b.append(trade.dtGmt.toStandardString)
        b.append("")
        b.append(trade.side)
        b.append("")
        b.append(trade.qty)
        b.append("")
        b.append(utils.dbl2Str(trade.price, 2))
        return b.toString()
    }

    def write(fileName: String, model: String, trade: Trade) = {
        statFileDumper.appendRow(fileName, SerializeTrade(model, trade))
    }
}

