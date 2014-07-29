package firelib.common


import java.text.DecimalFormat

import breeze.stats.DescriptiveStatsTrait

import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._



object Utils extends DescriptiveStatsTrait {


    def Dbl2Str(vv: Double, decPlaces: Int): String = {
        var dp = if (decPlaces > 0) "#." else "#";
        for (a <- 1 to decPlaces) {
            dp += "#";
        }
        val df = new DecimalFormat(dp);
        return df.format(vv)
    }


    private def GetTradingCasesInt(trades: Seq[Trade], positionTrades: ArrayBuffer[Trade]): Seq[(Trade, Trade)] = {
        var posTrades = new ArrayBuffer[Trade]();

        var tradingCases = new ArrayBuffer[(Trade, Trade)]();

        trades.foreach(t => {
            if (posTrades.length == 0) {
                posTrades += t
            }
            else {
                if (posTrades.last.TradeSide != t.TradeSide) {
                    var residualAmt = t.Qty;


                        breakable {
                            while (true) {
                                val lastPositionTrade = posTrades.last
                                posTrades.remove(posTrades.length - 1)

                                if (lastPositionTrade.Qty >= residualAmt) {
                                    val spl = lastPositionTrade.Split(residualAmt);
                                    tradingCases += ((spl._1, t.Split(residualAmt)._1));

                                    assert(tradingCases.last._1.TradeSide != tradingCases.last._2.TradeSide, "trading case must have different sides")

                                    if (spl._2 != null) {
                                        posTrades += spl._2
                                    }
                                    break;
                                }
                                residualAmt -= lastPositionTrade.Qty;
                                tradingCases += ((lastPositionTrade, t.Split(lastPositionTrade.Qty)._1));

                                if (posTrades.length == 0 && residualAmt > 0) {
                                    posTrades += t.Split(residualAmt)._1;
                                    break;
                                }
                                //if (residualAmt == 0) break;
                            }
                        }

                }
                else {
                    posTrades += t
                }
            }
            if (tradingCases.length > 0){
                assert(tradingCases.last._1.TradeSide != tradingCases.last._2.TradeSide,"trading case must have different sides")
            }
        });
        if (positionTrades != null) {
            positionTrades ++= posTrades;
        }

        tradingCases.sortBy(tu => tu._1.DtGmt.toEpochMilli);

        return tradingCases;
    }


    def PnlForCase(cas: (Trade, Trade)): Double = {
        return cas._1.moneyFlow + cas._2.moneyFlow;
    }

    def FilterTradingCases(tradingCases: Seq[(Trade, Trade)]): Seq[(Trade, Trade)] = {
        var std = variance(tradingCases.map(PnlForCase))
        return tradingCases.filter(tc => math.abs(PnlForCase(tc)) < 10 * std)
    }

    def GetTradingCases(trades: Seq[Trade], positionTrades: ArrayBuffer[Trade] = null): ArrayBuffer[(Trade, Trade)] = {
        var ret = new ArrayBuffer[(Trade, Trade)]();

        val groupedBySecurity: Map[String, Seq[Trade]] = trades.groupBy(t => t.Security)

        groupedBySecurity.foreach(group => {
            ret ++= FilterTradingCases(GetTradingCasesInt(group._2, positionTrades));
        })
        return ret;
    }


    /*


          def GetApplicationDataPath(fileName : String) : String
            {
                return ""
                //return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), fileName);
            }






            public static void SafeCallWithLog(Action action, Logger logger, string msg = null)
            {
                try
                {
                    action();
                }
                catch (Exception e)
                {
                    logger.Error(msg + ":" + e.Message + " " + e.StackTrace);
                }
            }


            public static string CaseToStr(Tuple<Trade, Trade> tuple)
            {
                var ret = new StringBuilder();
                ret.Append(tuple.Item1.Side);
                ret.Append('|').Append(tuple.Item1.DtGmt.ToStandardString());
                ret.Append('|').Append(tuple.Item2.DtGmt.ToStandardString());
                ret.Append('|').Append(Dbl2Str(tuple.Item1.Price, 4));
                ret.Append('|').Append(Dbl2Str(tuple.Item2.Price, 4));
                return ret.ToString();
            }

            public static string GetBaseDir()
            {
                var baseDir = Directory.GetCurrentDirectory();

                while (!baseDir.EndsWith("bin"))
                {
                    baseDir = Directory.GetParent(baseDir).FullName;
                }
                baseDir = NormPath(Path.Combine(baseDir, "../../"));
                return baseDir;
            }

            public static string NormPath(string path)
            {
                return Path.GetFullPath(path).Replace("\\", "/");
            }




            public static bool SafeOp(Action action)
            {
                try
                {
                    action();
                }
                catch (Exception e)
                {
                    Console.WriteLine("Exception while doing safe op : " + e.Message);
                    return false;
                }
                return true;
            }
    */


}
