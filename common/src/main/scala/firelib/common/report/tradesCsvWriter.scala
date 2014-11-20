package firelib.common.report

import firelib.common.misc.dateUtils._
import firelib.common.misc.{statFileDumper, utils}
import firelib.common.model.Model
import firelib.common.{Order, Side, Trade}

import scala.collection.mutable.ArrayBuffer

/**

 */
object tradesCsvWriter {

    val decPlaces: Int = 5
    val separator = ";"

    val colsDef = List[(String,((Trade,Trade))=>String)](
        ("Ticker",t=>t._1.security),
        ("OrderId0",t=>t._1.order.id),
        ("OrderId1",t=>t._2.order.id),
        ("BuySell",t=>if(t._1.side == Side.Buy) "1" else "-1"),
        ("EntryDate",t=>t._1.dtGmt.toStandardString),
        ("EntryPrice",t=>utils.dbl2Str(t._1.price, decPlaces)),
        ("ExitDate",t=>t._2.dtGmt.toStandardString),
        ("ExitPrice",t=>utils.dbl2Str(t._2.price, decPlaces)),
        ("Pnl",t=>utils.dbl2Str(utils.pnlForCase(t), decPlaces)),
        ("Qty",t=>t._1.qty.toString),
        ("MAE",t=>utils.dbl2Str(t._2.MAE, decPlaces)),
        ("MFE",t=>utils.dbl2Str(t._2.MFE, decPlaces))
    )
    
    def serializeTradeCase(trdCase : (Trade,Trade)) : List[String] = colsDef.map(_._2).map(_(trdCase))

    def write(model: Model, file: String, factorcols: Iterable[String]): Unit = {
        val tradingCases = utils.toTradingCases(model.trades)
        var rows = new ArrayBuffer[String]()
        rows += (colsDef.map(_._1) ++ factorcols).mkString(separator)
        for (t <- tradingCases) {
            rows += (serializeTradeCase(t) ++ factorcols.map(t._1.factors(_))).mkString(";")
        }
        statFileDumper.writeRows(file, rows)
    }


    val orderColsDef = List[(String,Order=>String)](
        ("Ticker",o=>o.security),
        ("OrderId",o=>o.id),
        ("OrderType",o=>o.orderType.Name),
        ("BuySell",o=>if(o.side == Side.Buy) "1" else "-1"),
        ("EntryDate",o=>o.placementTime.toStandardString),
        ("Price",o=>utils.dbl2Str(o.price, decPlaces)),
        ("Qty",o=>o.qty.toString)
    )

    def serializeOrder(order : Order) : List[String] = orderColsDef.map(_._2).map(_(order))

    def writeOrders(orders: Seq[Order], file: String): Unit = {
        var rows = new ArrayBuffer[String]()
        rows += (orderColsDef.map(_._1) ).mkString(separator)
        for (t <- orders) {
            rows += serializeOrder(t).mkString(";")
        }
        statFileDumper.writeRows(file, rows)
    }
}
