package firelib.common


import java.time._
import java.time.format.DateTimeFormatter

import firelib.backtest.BacktestStarter
import firelib.utils.DateTimeExt
import firelib.utils.DateTimeExt._
import org.junit.{Assert, Test}

import scala.collection.mutable.ArrayBuffer


@Test
class BacktestIntegrationTest {

    val zoneId = ZoneId.of("America/New_York")

    def getTime(y: Int, month: Int, d: Int, h: Int, m: Int, s: Int, mil: Int): Instant = {
        val ret: Instant = LocalDateTime.of(y, month, d, h, m, s, mil * 1000000).atZone(zoneId).toInstant
        val ret1: Instant = LocalDateTime.of(y, month, d, h, m, s, mil * 1000000).atZone(ZoneId.of("UTC")).toInstant
        return ret
    }

    def getDsRoot(): String = {
        return "/home/ivan/tmp/testDsRoot"
    }


    @Test
    def IntegrationTestTestTicks() = {

        val fileName: String = "TICKS/XG_#.csv"

        var d0Gmt = LocalDateTime.of(2013, 3, 8, 5, 0, 0, 0).toInstant(ZoneOffset.UTC)

        var d0 = getTime(2013, 3, 8, 0, 0, 0, 0)

        Assert.assertTrue(d0 == d0Gmt)
        var d1 = getTime(2013, 3, 9, 0, 0, 0, 0)

        var d2 = getTime(2013, 3, 11, 0, 0, 0, 0)
        var d3 = getTime(2013, 3, 12, 0, 0, 0, 0)

/*
        Files.deleteIfExists(Paths.get(getDsRoot() + "/" + fileName))

        Files.createDirectories(Paths.get(getDsRoot() + "/" + fileName).getParent)

        Files.createFile(Paths.get(getDsRoot() + "/" + fileName))
*/


        val lst =  WriteInterval(d0.atZone(zoneId).toLocalDateTime, d1.atZone(zoneId).toLocalDateTime, 500,  tickGen)

//        Files.write(Paths.get(getDsRoot() + "/" + fileName),lst.toStream, StandardOpenOption.WRITE)

        var lst2 = WriteInterval(d2.atZone(zoneId).toLocalDateTime, d3.atZone(zoneId).toLocalDateTime, 500, tickGen)

  //      Files.write(Paths.get(getDsRoot() + "/" + fileName),lst2.toStream, StandardOpenOption.APPEND)


        var cfg = new ModelConfig()

        cfg.dataServerRoot = getDsRoot()
        cfg.reportRoot = getDsRoot() + "/reportRoot"


        cfg.addTickerId(new TickerConfig("XG", fileName, MarketDataType.Tick))

        //??? ??? ????? ?? 2012-03-07 22:00:00 ?? ????????
        cfg.startDateGmt = "2013-03-08 05:00:00"

        cfg.intervalName = Interval.Sec1.Name

        var startTime = cfg.startDateGmt.toDtGmt

        cfg.className = "firelib.common.TestModel"

        BacktestStarter.Start(cfg)


        //less by one because we read next at the same time
        Assert.assertTrue("ticks number not match " + TestHelper.instance.NumberOfTickes + " <> " + (lst.length + lst2.length - 1), TestHelper.instance.NumberOfTickes == (lst.length + lst2.length - 1))
        Assert.assertTrue("days number", TestHelper.instance.startTimesGmt.length == 4)
        Assert.assertTrue("time check", TestHelper.instance.startTimesGmt(0) == d0)
        //????? ????? ?????
        Assert.assertEquals(d2, TestHelper.instance.startTimesGmt(2) )

        //2 h without ticks
        //Assert.assertEquals(TimeSpan.FromHours(0), d0.ToGmt("NY") - startTime)

        var cursor = startTime
        var idx = 0
        while (cursor.isBefore(d0)) {

            var bar = TestHelper.instance.bars(idx)
            idx += 1
            Assert.assertTrue("times not match " + bar.DtGmtEnd + " <> " + cursor, bar.DtGmtEnd == cursor)
            Assert.assertTrue("not interpolated idx = " + idx, bar.Interpolated)

            cursor = cursor.plusSeconds(5*60)
        }

        while (d1.isAfter(cursor)) {
            var bar = TestHelper.instance.bars(idx)
            idx += 1
            Assert.assertTrue("times not match " + bar.DtGmtEnd + " <> " + cursor, bar.DtGmtEnd == cursor)
            Assert.assertTrue("not interpolated idx = " + idx, !bar.Interpolated)
            cursor = cursor.plusSeconds(5*60)
        }
    }


    /*

            @Test
            def IntegrationTestTestMins()
            {
                // ??? ??????? ??????????? ?????
                var d0Gmt = getTime(2013, 03, 08, 5, 0, 0, 0)
                var d0 = getTime(2013, 03, 08, 0, 0, 0, 0)
                Assert.assertTrue(d0 == d0Gmt.FromGmt("NY"))
                var d1 = getTime(2013, 03, 9, 0, 0, 0, 0)

                var d2 = getTime(2013, 03, 11, 0, 0, 0, 0)
                var d3 = getTime(2013, 03, 12, 0, 0, 0, 0)


                WriteInterval1Min(d0, d1, w1, 5*60*1000)
                WriteInterval1Min(d2, d3, w1, 5*60*1000)



                var pp = new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, @"TestData\1MIN\1_#.csv"))

                pp.SeekLocal(d0)

                var directBars = new List<Ohlc>()

                while (pp.Read())
                {
                    unsafe
                    {
                        var ohlc = (*pp.PQuote).ToOhlc()
                        ohlc.DtGmtEnd = ohlc.DtGmtEnd.ToGmt("NY")
                        directBars.Add(ohlc)
                    }
                }

                var starter = new BacktesterSimple()
                var cfg = new ModelConfig()

                cfg.DataServerRoot = strTestsRootPath
                cfg.BinaryStorageRoot = strTestsRootPath
                cfg.ReportRoot = strTestsRootPath
                cfg.AddTickerId(new TickerConfig
                                    {
                                        Path = @"TestData\1MIN\1_#.csv",
                                        TickerId = "XG",
                                        TickerType = TickerType.Ohlc
                                    })

                //??? ??? ????? ?? 2012-03-07 22:00:00 ?? ????????
                cfg.StartDateGmt = "2013-03-08 05:00:00"

                cfg.IntervalName = Interval.Sec1.Name

                var startTime = DateTimeExtension.ParseStandard(cfg.StartDateGmt)

                cfg.ClassName = "TestModelOhlc"

                starter.Run(cfg)


                int idx = -1
                var modelBars = TestModelOhlc.instance.bars
                DateTime curTime = startTime
                for (int i = 0 i < modelBars.Count i++)
                {
                    Assert.assertEquals(curTime,modelBars[i].DtGmtEnd)
                    curTime =curTime.AddMinutes(5)
                    if (!modelBars[i].Interpolated)
                    {
                        idx++
                    }
                    if (idx != -1)
                    {
                        Ohlc rb = directBars[idx]
                        Assert.assertEquals(rb.C, modelBars[i].C, "idx " + idx)
                        Assert.assertEquals(rb.O, modelBars[i].O, "idx " + idx)
                        Assert.assertEquals(rb.H, modelBars[i].H, "idx " + idx)
                        Assert.assertEquals(rb.L, modelBars[i].L, "idx " + idx)

                    }
                }

                Assert.assertTrue(TestModelOhlc.instance.startTimesGmt.Count == 5, "days number")
            }
    */


    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy,HHmmss.SSS")

    def WriteInterval(startTime: LocalDateTime, endTime: LocalDateTime, stepMillis: Int, strGen : LocalDateTime=>String): Seq[String] = {
        var cnt = 0
        var cursor = startTime
        val lst = new ArrayBuffer[String]()
        while (endTime.isAfter(cursor)) {
            lst += strGen(cursor)
            cursor = cursor.plusNanos(stepMillis * 1000000)
            cnt += 1
        }
        return lst
    }


    def ohlcGen(cursor: LocalDateTime) : String = {
        var cl = cursor.toLocalTime.toSecondOfDay + 10
        var close = "%.2f".format(cl)
        var high = "%.2f".format(cl + 2)
        var low = "%.2f".format(cl - 2)
        var open = close
        val dt = formatter.format(cursor)
        return s"$dt,$open,$high,$low,$close,1000,1"
    }

    def tickGen(cursor: LocalDateTime) : String = {
        var cl = cursor.toLocalTime.toSecondOfDay.toDouble
        var last = "%.2f".format(cl)
        var bid =  "%.2f".format(cl + 0.1)
        var ask =  "%.2f".format(cl + 0.2)
        val dt = formatter.format(cursor)
        return s"$dt,$last,1,1,$bid,$ask,1"
    }

}
