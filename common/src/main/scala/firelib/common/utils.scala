package firelib.common


import java.text.DecimalFormat

import breeze.stats.DescriptiveStatsTrait

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer



object utils extends DescriptiveStatsTrait {


    def dbl2Str(vv: Double, decPlaces: Int): String = {
        var dp = if (decPlaces > 0) "#." else "#"
        for (a <- 1 to decPlaces) {
            dp += "#"
        }
        val df = new DecimalFormat(dp)
        return df.format(vv)
    }


    private def toTradingCasesInt(trades: Seq[Trade], positionTrades: ArrayBuffer[Trade]): Seq[(Trade, Trade)] = {
        val posTrades = new mutable.Stack[Trade]()
        val tradingCases = new ArrayBuffer[(Trade, Trade)]()

        trades.foreach(trade => makeCaseWithPositionTrades(posTrades, tradingCases, trade))

        tradingCases.foreach(tc=>assert(tc._1.side != tc._2.side,"trading case must have different sides"))

        if (positionTrades != null) {
            positionTrades ++= posTrades
        }

        tradingCases.sortBy(_._1.dtGmt.toEpochMilli)

        return tradingCases
    }


    def makeCaseWithPositionTrades(posTrades: mutable.Stack[Trade], tradingCases: ArrayBuffer[(Trade, Trade)], trade: Trade) {
        if(posTrades.isEmpty || posTrades.top.side == trade.side){
            posTrades.push(trade)
            return
        }
        var residualAmt = trade.qty
        val lastPositionTrade = posTrades.pop()
        if (lastPositionTrade.qty >= residualAmt) {
            val tradeSplit = lastPositionTrade.split(residualAmt)
            tradingCases += ((tradeSplit._1, trade.split(residualAmt)._1))
            if (tradeSplit._2.qty != 0) posTrades.push(tradeSplit._2)
        }else{
            residualAmt -= lastPositionTrade.qty
            val tradeSplit: (Trade, Trade) = trade.split(lastPositionTrade.qty)
            tradingCases += ((lastPositionTrade, tradeSplit._1))
            makeCaseWithPositionTrades(posTrades, tradingCases,tradeSplit._2)
        }
    }

    def pnlForCase(cas: (Trade, Trade)): Double = cas._1.moneyFlow + cas._2.moneyFlow

    val filterThresholdInVariance: Int = 10

    private def filterTradingCases(tradingCases: Seq[(Trade, Trade)]): Seq[(Trade, Trade)] = {
        val std = variance(tradingCases.map(pnlForCase))
        return tradingCases.filter(tc => math.abs(pnlForCase(tc)) < filterThresholdInVariance * std)
    }

    def toTradingCases(trades: Seq[Trade], positionTrades: ArrayBuffer[Trade] = null): ArrayBuffer[(Trade, Trade)] = {
        var ret = new ArrayBuffer[(Trade, Trade)]()

        val groupedBySecurity: Map[String, Seq[Trade]] = trades.groupBy(t => t.security)

        groupedBySecurity.foreach(group => {
            ret ++= filterTradingCases(toTradingCasesInt(group._2, positionTrades))
        })
        return ret
    }


    /*


          def GetApplicationDataPath(fileName : String) : String
            {
                return ""
                //return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), fileName)
            }






            public static void SafeCallWithLog(Action action, Logger logger, string msg = null)
            {
                try
                {
                    action()
                }
                catch (Exception e)
                {
                    logger.Error(msg + ":" + e.Message + " " + e.StackTrace)
                }
            }


            public static string CaseToStr(Tuple<Trade, Trade> tuple)
            {
                var ret = new StringBuilder()
                ret.Append(tuple.Item1.Side)
                ret.Append('|').Append(tuple.Item1.DtGmt.ToStandardString())
                ret.Append('|').Append(tuple.Item2.DtGmt.ToStandardString())
                ret.Append('|').Append(Dbl2Str(tuple.Item1.Price, 4))
                ret.Append('|').Append(Dbl2Str(tuple.Item2.Price, 4))
                return ret.ToString()
            }

            public static string GetBaseDir()
            {
                var baseDir = Directory.GetCurrentDirectory()

                while (!baseDir.EndsWith("bin"))
                {
                    baseDir = Directory.GetParent(baseDir).FullName
                }
                baseDir = NormPath(Path.Combine(baseDir, "../../"))
                return baseDir
            }

            public static string NormPath(string path)
            {
                return Path.GetFullPath(path).Replace("\\", "/")
            }




            public static bool SafeOp(Action action)
            {
                try
                {
                    action()
                }
                catch (Exception e)
                {
                    Console.WriteLine("Exception while doing safe op : " + e.Message)
                    return false
                }
                return true
            }
    */


}