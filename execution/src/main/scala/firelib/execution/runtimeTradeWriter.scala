package firelib.execution

import firelib.common._
import firelib.common.misc.dateUtils._
import firelib.common.misc.{dateUtils, statFileDumper, utils}

object runtimeTradeWriter {

    def SerializeTrade(model: String, trade: Trade): String = {
        var b = new StringBuilder()
        b.append(model)
        b.append(",")
        b.append(trade.security)
        b.append(",")
        b.append(trade.order.id)
        b.append(",")
        b.append(trade.dtGmt.toStandardString)
        b.append(",")
        b.append(trade.side)
        b.append(",")
        b.append(trade.qty)
        b.append(",")
        b.append(utils.dbl2Str(trade.price, 6))
        return b.toString()
    }

    def write(fileName: String, model: String, trade: Trade) = {
        statFileDumper.appendRow(fileName, SerializeTrade(model, trade))
    }
}

