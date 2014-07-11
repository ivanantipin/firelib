package com.firelib.util

import java.text.DecimalFormat

object Utils {

  def Dbl2Str(vv: Double, decPlaces: Int): String = {
    var dp = if (decPlaces > 0) "#." else "#";
    for (a <- 1 to decPlaces) {
      dp += "#";
    }
    val df = new DecimalFormat(dp);
    return df.format(vv)
  }

  /*


        def GetApplicationDataPath(fileName : String) : String
          {
              return ""
              //return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), fileName);
          }

          public static List<Tuple<Trade, Trade>> FilterTradingCases(List<Tuple<Trade, Trade>> tradingCases)
          {
              var std = tradingCases.Select(PnlForCase).StandardDeviation();
              var tradingCasesFiltered = new List<Tuple<Trade,Trade>>(tradingCases.Where(tc => Math.Abs(PnlForCase(tc)) < 100*std));
              return tradingCasesFiltered;
          }

          public static List<Tuple<Trade, Trade>> GetTradingCases(List<Trade> trades, List<Trade> positionTrades = null)
          {
              var ret = new List<Tuple<Trade, Trade>>();
              foreach (var ts in trades.GroupBy(item => item.Security))
              {
                  ret.AddRange(FilterTradingCases(GetTradingCasesInt(new List<Trade>(ts), positionTrades)));
              }
              return ret;
          }

          private static List<Tuple<Trade, Trade>> GetTradingCasesInt(List<Trade> trades, List<Trade> positionTrades)
          {
              var posTrades = new LinkedList<Trade>();

              var tradingCases = new List<Tuple<Trade, Trade>>();

              trades.ForEach(t =>
                                 {
                                     if (posTrades.Count == 0)
                                     {
                                         posTrades.AddLast(t);
                                     }
                                     else
                                     {
                                         if (posTrades.Last().Side != t.Side)
                                         {
                                             int residualAmt = t.Qty;
                                             while (true)
                                             {
                                                 Trade lastPositionTrade = posTrades.Last();
                                                 posTrades.RemoveLast();
                                                 if (lastPositionTrade.Qty >= residualAmt)
                                                 {
                                                     Tuple<Trade, Trade> spl = lastPositionTrade.Split(residualAmt);
                                                     tradingCases.Add(new Tuple<Trade, Trade>(spl.Item1,
                                                                                              t.Split(residualAmt).Item1));
                                                     if (tradingCases.Last().Item1.Side == tradingCases.Last().Item2.Side)
                                                     {
                                                         throw new Exception("trading case must have different sides");
                                                     }

                                                     if (spl.Item2 != null)
                                                     {
                                                         posTrades.AddLast(spl.Item2);
                                                     }
                                                     break;
                                                 }
                                                 residualAmt -= lastPositionTrade.Qty;
                                                 tradingCases.Add(new Tuple<Trade, Trade>(lastPositionTrade,
                                                                                          t.Split(lastPositionTrade.Qty)
                                                                                           .Item1));
                                                 if (posTrades.Count == 0 && residualAmt > 0)
                                                 {
                                                     posTrades.AddLast(t.Split(residualAmt).Item1);
                                                     break;
                                                 }
                                                 //if (residualAmt == 0) break;
                                             }
                                         }
                                         else
                                         {
                                             posTrades.AddLast(t);
                                         }
                                     }
                                     if (tradingCases.Count > 0 &&
                                         tradingCases.Last().Item1.Side == tradingCases.Last().Item2.Side)
                                     {
                                         throw new Exception("trading case must have different sides");
                                     }
                                 });
              if (positionTrades != null)
              {
                  positionTrades.AddRange(posTrades);
              }

              tradingCases.Sort((t0, t1) => t0.Item1.DtGmt.CompareTo(t1.Item1.DtGmt));

              return tradingCases;
          }

          public static double PnlForCase(Tuple<Trade, Trade> cas)
          {
              int sign = cas.Item1.Side == Side.Buy ? -1 : 1;
              return cas.Item1.Price*cas.Item1.Qty*sign - cas.Item2.Price*cas.Item2.Qty*sign;
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
