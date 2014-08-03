using System;
using System.Collections.Generic;
using System.IO;
using Fire.Common.Backtest;
using Fire.Common.Domain;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using QuantLib.Domain;
using QuantLib.Helpers;
using TFWTests;
using UltraFastParser.TimeZone;
using Assert = Microsoft.VisualStudio.TestTools.UnitTesting.Assert;

namespace Tests
{
    [TestClass]
    public class BacktestIntegrationTest
    {
        private static string strTestsRootPath = @"..\..\..\";


        [TestMethod]
        public void IntegrationTestTestTicks()
        {
            // это местное ньюйоркское время
            var d0Gmt = new DateTime(2013, 03, 08, 5, 0, 0, 0);
            var d0 = new DateTime(2013, 03, 08, 0, 0, 0, 0);
            Assert.IsTrue(d0 == d0Gmt.FromGmt("NY"));
            var d1 = new DateTime(2013, 03, 9, 0, 0, 0, 0);

            var d2 = new DateTime(2013, 03, 11, 0, 0, 0, 0);
            var d3 = new DateTime(2013, 03, 12, 0, 0, 0, 0);


            var w1 = new StreamWriter(Path.Combine(strTestsRootPath, @"TestData\TICKS\1_#.csv"));
            int cnt = WriteInterval(d0, d1, w1,500);

            var cnt2 = WriteInterval(d2, d3, w1,500);

            w1.Flush();
            w1.Close();


            var starter = new BacktesterSimple();
            var cfg = new ModelConfig();

            cfg.DataServerRoot = strTestsRootPath;
            cfg.BinaryStorageRoot = strTestsRootPath;
            cfg.ReportRoot = strTestsRootPath;
            cfg.AddTickerId(new TickerConfig
                                {
                                    Path = @"TestData\TICKS\1_#.csv",
                                    TickerId = "XG",
                                    TickerType = TickerType.Tick
                                });

            //это гмт часть те 2012-03-07 22:00:00 по ньюйорку
            cfg.StartDateGmt = "2013-03-08 05:00:00";

            cfg.IntervalName = Interval.Sec1.Name;

            var startTime = DateTimeExtension.ParseStandard(cfg.StartDateGmt);

            cfg.ClassName = "TestModel";

            starter.Run(cfg);


            Assert.IsTrue(TestModel.instance.NumberOfTickes == cnt + cnt2, "ticks number");
            Assert.IsTrue(TestModel.instance.startTimesGmt.Count == 4, "days number");
            Assert.IsTrue(TestModel.instance.startTimesGmt[0] == d0.ToGmt("NY"), "time check");
            //время после ролла
            Assert.AreEqual(d2.ToGmt("NY"), TestModel.instance.startTimesGmt[2]);

            //2 h without ticks
            Assert.AreEqual(TimeSpan.FromHours(0), d0.ToGmt("NY") - startTime);

            var cursor = startTime;
            int idx = 0;
            while (cursor < d0.ToGmt("NY"))
            {
                var bar = TestModel.instance.bars[idx++];
                Assert.IsTrue(bar.DtGmtEnd == cursor, "times not match " + bar.DtGmtEnd + " <> " + cursor);
                Assert.IsTrue(bar.Interpolated, "not interpolated idx = " + idx);
                cursor = cursor.Add(TimeSpan.FromMinutes(5));
            }

            while (cursor <= d1)
            {
                var bar = TestModel.instance.bars[idx++];
                Assert.IsTrue(bar.DtGmtEnd == cursor, "times not match " + bar.DtGmtEnd + " <> " + cursor);
                Assert.IsTrue(!bar.Interpolated, "not interpolated idx = " + idx);
                cursor = cursor.Add(TimeSpan.FromMinutes(5));
            }
        }




        [TestMethod]
        public void IntegrationTestTestMins()
        {
            // это местное ньюйоркское время
            var d0Gmt = new DateTime(2013, 03, 08, 5, 0, 0, 0);
            var d0 = new DateTime(2013, 03, 08, 0, 0, 0, 0);
            Assert.IsTrue(d0 == d0Gmt.FromGmt("NY"));
            var d1 = new DateTime(2013, 03, 9, 0, 0, 0, 0);

            var d2 = new DateTime(2013, 03, 11, 0, 0, 0, 0);
            var d3 = new DateTime(2013, 03, 12, 0, 0, 0, 0);


            var w1 = new StreamWriter(Path.Combine(strTestsRootPath, @"TestData\1MIN\1_#.csv"));
            WriteInterval1Min(d0, d1, w1, 5*60*1000);
            WriteInterval1Min(d2, d3, w1, 5*60*1000);

            

            w1.Flush();
            w1.Close();

            var pp = new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, @"TestData\1MIN\1_#.csv"));

            pp.SeekLocal(d0);

            var directBars = new List<Ohlc>();

            while (pp.Read())
            {
                unsafe
                {
                    var ohlc = (*pp.PQuote).ToOhlc();
                    ohlc.DtGmtEnd = ohlc.DtGmtEnd.ToGmt("NY");
                    directBars.Add(ohlc);
                }
            }

            var starter = new BacktesterSimple();
            var cfg = new ModelConfig();

            cfg.DataServerRoot = strTestsRootPath;
            cfg.BinaryStorageRoot = strTestsRootPath;
            cfg.ReportRoot = strTestsRootPath;
            cfg.AddTickerId(new TickerConfig
                                {
                                    Path = @"TestData\1MIN\1_#.csv",
                                    TickerId = "XG",
                                    TickerType = TickerType.Ohlc
                                });

            //это гмт часть те 2012-03-07 22:00:00 по ньюйорку
            cfg.StartDateGmt = "2013-03-08 05:00:00";

            cfg.IntervalName = Interval.Sec1.Name;

            var startTime = DateTimeExtension.ParseStandard(cfg.StartDateGmt);

            cfg.ClassName = "TestModelOhlc";

            starter.Run(cfg);


            int idx = -1;
            var modelBars = TestModelOhlc.instance.bars;
            DateTime curTime = startTime;
            for (int i = 0; i < modelBars.Count; i++)
            {
                Assert.AreEqual(curTime,modelBars[i].DtGmtEnd);
                curTime =curTime.AddMinutes(5);
                if (!modelBars[i].Interpolated)
                {
                    idx++;
                }
                if (idx != -1)
                {
                    Ohlc rb = directBars[idx];
                    Assert.AreEqual(rb.C, modelBars[i].C, "idx " + idx);
                    Assert.AreEqual(rb.O, modelBars[i].O, "idx " + idx);
                    Assert.AreEqual(rb.H, modelBars[i].H, "idx " + idx);
                    Assert.AreEqual(rb.L, modelBars[i].L, "idx " + idx);

                }
            }

            Assert.IsTrue(TestModelOhlc.instance.startTimesGmt.Count == 5, "days number");
        }


        private static int WriteInterval(DateTime cursor, DateTime endTime, StreamWriter w1, int stepMillis)
        {
            int cnt = 0;
            while (cursor <= endTime)
            {
                var last = (cursor.TimeOfDay.TotalMilliseconds/1000.0).ToString("0.##");
                var bid = ((cursor.TimeOfDay.TotalMilliseconds + 100)/1000.0).ToString("0.##");
                var ask = ((cursor.TimeOfDay.TotalMilliseconds + 200)/1000.0).ToString("0.##");
                w1.WriteLine(String.Format("{0}.{1}.{2},{3}{4}{5}.{6},{7},{8},{9},{10},{11}",
                                           cursor.Day.ToString("00"), cursor.Month.ToString("00"), cursor.Year, cursor.Hour.ToString("00"),
                                           cursor.Minute.ToString("00"), cursor.Second.ToString("00"), cursor.Millisecond.ToString("000")
                                           , last, 1, 1, bid, ask));
                cursor = cursor.AddMilliseconds(stepMillis);
                cnt++;
            }

            return cnt;
        }

        private static int WriteInterval1Min(DateTime cursor, DateTime endTime, StreamWriter w1, int stepMillis)
        {
            int cnt = 0;
            while (cursor <= endTime)
            {
                var cl = cursor.TimeOfDay.TotalMilliseconds/1000.0 + 10;
                var close = cl.ToString("0.##");
                var high = (cl + 2).ToString("0.##");
                var low = (cl - 2).ToString("0.##");
                var open = close;
                w1.WriteLine(String.Format("{0}.{1}.{2},{3}{4}{5},{6},{7},{8},{9},{10},{11}",
                                           cursor.Day.ToString("00"), cursor.Month.ToString("00"), cursor.Year, cursor.Hour.ToString("00"),
                                           cursor.Minute.ToString("00"), cursor.Second.ToString("00")
                                           , open, high, low,close, 1000,1));
                cursor = cursor.AddMilliseconds(stepMillis);
                cnt++;
            }

            return cnt;
        }


    }
}