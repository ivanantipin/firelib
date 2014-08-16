package firelib.common


import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.time._
import java.time.format.DateTimeFormatter
import java.util.function.Supplier

import firelib.backtest.backtestStarter
import firelib.domain.Ohlc
import firelib.parser.{CommonIniSettings, IHandler, Parser, ParserHandlersProducer}
import firelib.utils.DateTimeExt._
import org.junit.{Assert, Test}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer


@Test
class BacktestIntegrationTest {

    val zoneId = ZoneId.of("America/New_York")

    def getUsTime(y: Int, month: Int, d: Int, h: Int, m: Int, s: Int, mil: Int): Instant = {
        val ret: Instant = LocalDateTime.of(y, month, d, h, m, s, mil * 1000000).atZone(zoneId).toInstant
        return ret
    }

    def getDsRoot(): String = {
        return "/home/ivan/tmp/testDsRoot"
    }


    @Test
    def IntegrationTestTestTicks() = {

        val fileName: String = "TICKS/XG_#.csv"
        val fullFileName: Path = Paths.get(getDsRoot() + "/" + fileName)
        val iniPath: Path = fullFileName.getParent.resolve("common.ini")

        var d0Gmt = LocalDateTime.of(2013, 3, 8, 5, 0, 0, 0).toInstant(ZoneOffset.UTC)

        var d0 = getUsTime(2013, 3, 8, 0, 0, 0, 0)

        Assert.assertTrue(d0 == d0Gmt)
        var d1 = getUsTime(2013, 3, 9, 0, 0, 0, 0)

        var d2 = getUsTime(2013, 3, 11, 0, 0, 0, 0)
        var d3 = getUsTime(2013, 3, 12, 0, 0, 0, 0)



        val quotesNumbers: (Int, Int) = createFiles(fullFileName, d0, d1, d2, d3, tickGen, 500)

        var totalTicksNumber = quotesNumbers._1 + quotesNumbers._2;


        var cfg = new ModelConfig()

        cfg.dataServerRoot = getDsRoot()
        cfg.reportRoot = "/home/ivan/tmp/testReportDir"
        cfg.tickerConfigs += new TickerConfig("XG", fileName, MarketDataType.Tick)
        cfg.startDateGmt = "08.03.2013 05:00:00"
        cfg.frequencyIntervalId = Interval.Sec1.Name
        var startTime = cfg.startDateGmt.parseStandard
        cfg.className = "firelib.common.TickTestModel"
        backtestStarter.Start(cfg)


        //less by one because we read next at the same time
        Assert.assertTrue("ticks number not match " + testHelper.instanceTick.NumberOfTickes + " <> " + (totalTicksNumber - 1), testHelper.instanceTick.NumberOfTickes == (totalTicksNumber - 1))
        Assert.assertTrue("days number", testHelper.instanceTick.startTimesGmt.length == 4)
        Assert.assertTrue("time check", testHelper.instanceTick.startTimesGmt(0) == d0)
        //????? ????? ?????
        Assert.assertEquals(d2, testHelper.instanceTick.startTimesGmt(2))

        //2 h without ticks
        //Assert.assertEquals(TimeSpan.FromHours(0), d0.ToGmt("NY") - startTime)

        var cursor = startTime
        var idx = 0
        while (cursor.isBefore(d0)) {
            var bar = testHelper.instanceTick.bars(idx)
            idx += 1
            Assert.assertTrue("times not match " + bar.dtGmtEnd + " <> " + cursor, bar.dtGmtEnd == cursor)
            Assert.assertTrue("not interpolated idx = " + idx, bar.interpolated)

            cursor = cursor.plusSeconds(5 * 60)
        }

        while (d1.isAfter(cursor)) {
            var bar = testHelper.instanceTick.bars(idx)
            idx += 1
            Assert.assertTrue("times not match " + bar.dtGmtEnd + " <> " + cursor, bar.dtGmtEnd == cursor)
            Assert.assertTrue("not interpolated idx = " + idx, !bar.interpolated)
            cursor = cursor.plusSeconds(5 * 60)
        }
    }


    @Test
    def IntegrationTestTestMins() {


        val fileName: String = "MINS/XG_#.csv"
        val fullFileName: Path = Paths.get(getDsRoot() + "/" + fileName)
        val iniPath: Path = fullFileName.getParent.resolve("common.ini")

        var d0Gmt = LocalDateTime.of(2013, 3, 8, 5, 0, 0, 0).toInstant(ZoneOffset.UTC)

        var d0 = getUsTime(2013, 3, 8, 0, 0, 0, 0)

        Assert.assertTrue(d0 == d0Gmt)
        var d1 = getUsTime(2013, 3, 9, 0, 0, 0, 0)

        var d2 = getUsTime(2013, 3, 11, 0, 0, 0, 0)
        var d3 = getUsTime(2013, 3, 12, 0, 0, 0, 0)



        val quotesNumbers: (Int, Int) = createFiles(fullFileName, d0, d1, d2, d3, ohlcGen, 5 * 60 * 1000)

        var totalQuotesNumber = quotesNumbers._1 + quotesNumbers._2;


        val generator: ParserHandlersProducer = new ParserHandlersProducer(new CommonIniSettings().loadFromFile(iniPath.toString))

        val ohlcFactory = new Supplier[Ohlc] {
            override def get(): Ohlc = return new Ohlc()
        }


        val pp = new Parser[Ohlc](fullFileName.toString, generator.handlers.asInstanceOf[Array[IHandler[Ohlc]]], ohlcFactory)

        pp.seek(d0)

        var directBars = new ArrayBuffer[Ohlc]()

        do {
            directBars += pp.current()
        } while (pp.read())


        val cfg = new ModelConfig()


        cfg.dataServerRoot = getDsRoot()
        cfg.reportRoot = "/home/ivan/tmp/testReportDir"


        cfg.tickerConfigs += new TickerConfig("XG", fileName, MarketDataType.Ohlc)

        cfg.startDateGmt = "08.03.2013 05:00:00"

        cfg.frequencyIntervalId = Interval.Sec1.Name

        val startTime = cfg.startDateGmt.parseStandard

        cfg.className = "firelib.common.OhlcTestModel"


        backtestStarter.Start(cfg)

        var idx = -1
        val modelBars = testHelper.instanceOhlc.bars

        Assert.assertTrue("bars number", testHelper.instanceOhlc.bars.filter(!_.interpolated).size  == totalQuotesNumber - 1)

        var curTime = startTime
        for (i <- 0 until modelBars.size) {
            Assert.assertEquals(curTime, modelBars(i).dtGmtEnd)
            curTime = curTime.plusSeconds(5 * 60)
            if (!modelBars(i).interpolated) {
                idx += 1
            }
            if (idx != -1) {
                val rb = directBars(idx)
                Assert.assertEquals(rb.O, modelBars(i).O, 0.00001)
                Assert.assertEquals(rb.H, modelBars(i).H, 0.00001)
                Assert.assertEquals(rb.L, modelBars(i).L, 0.00001)
                Assert.assertEquals(rb.C, modelBars(i).C, 0.00001)
            }
        }

        Assert.assertTrue("days number", testHelper.instanceOhlc.startTimesGmt.size == 5)
    }


    def createFiles(fullFileName: Path, d0: Instant, d1: Instant, d2: Instant, d3: Instant, strGen: LocalDateTime => String, interval: Long, writeToDisk : Boolean = false) : (Int,Int) = {

        if(writeToDisk){
            Files.deleteIfExists(fullFileName)

            Files.createDirectories(fullFileName.getParent)

            Files.createFile(fullFileName)
        }


        val lst = generateInterval(d0.atZone(zoneId).toLocalDateTime, d1.atZone(zoneId).toLocalDateTime, interval, strGen)

        if(writeToDisk)
            Files.write(fullFileName, lst.toStream, StandardOpenOption.WRITE)

        var lst2 = generateInterval(d2.atZone(zoneId).toLocalDateTime, d3.atZone(zoneId).toLocalDateTime, interval, strGen)

        if(writeToDisk)
            Files.write(fullFileName, lst2.toStream, StandardOpenOption.APPEND)

        return (lst.length, lst2.length)
    }

    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy,HHmmss.SSS")

    def generateInterval(startTime: LocalDateTime, endTime: LocalDateTime, stepMillis: Long, strGen: LocalDateTime => String): Seq[String] = {
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


    def ohlcGen(cursor: LocalDateTime): String = {
        var cl = cursor.toLocalTime.toSecondOfDay.toDouble + 10
        var close = "%.2f".format(cl)
        var high = "%.2f".format(cl + 2)
        var low = "%.2f".format(cl - 2)
        var open = close
        val dt = formatter.format(cursor)
        return s"$dt,$open,$high,$low,$close,1000,1"
    }

    def tickGen(cursor: LocalDateTime): String = {
        var cl = cursor.toLocalTime.toSecondOfDay.toDouble
        var last = "%.2f".format(cl)
        var bid = "%.2f".format(cl + 0.1)
        var ask = "%.2f".format(cl + 0.2)
        val dt = formatter.format(cursor)
        return s"$dt,$last,1,1,$bid,$ask,1"
    }

}
