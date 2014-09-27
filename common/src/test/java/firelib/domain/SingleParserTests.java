package firelib.domain;

import firelib.common.misc.dateUtils;
import firelib.parser.CommonIniSettings;
import firelib.parser.Parser;
import firelib.parser.ParserHandlersProducer;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


public class SingleParserTests {

    /**
     * using testresources folder to prevent files copying every build
     */
    String getfile(String name) {
        return Paths.get("./common/src/test/testresources", name).toAbsolutePath().toString();
    }

    private ParserHandlersProducer getStdNyTickSettings() {
        CommonIniSettings commonIniSettings = new CommonIniSettings();
        commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
        commonIniSettings.TIMEFORMAT = "HHMMSS";
        commonIniSettings.COLUMNFORMAT = new String[]{"D", "T", "#", "P", "V", "U", "B", "A", "I"};
        commonIniSettings.TIMEZONE = "NY";
        return new ParserHandlersProducer(commonIniSettings);
    }

    private ParserHandlersProducer getStdFortsTickSettings() {
        //
        // Check that FORTS FUT format parsing works.
        //
        CommonIniSettings commonIniSettings = new CommonIniSettings();
        commonIniSettings.DATEFORMAT = "YYYY-MM-DD";
        commonIniSettings.TIMEFORMAT = "HH:MM:SS";
        commonIniSettings.COLUMNFORMAT = new String[] {"D", "T", "#", "P", "V", "I"};
        commonIniSettings.TIMEZONE = "MOSCOW";
        return new ParserHandlersProducer(commonIniSettings);
    }

    @Test
    public void TestUltraFastSingleCsvParserStartEndTimes() {


        ParserHandlersProducer parserHandlersProducer = getStdNyTickSettings();

        Parser<Tick> parser = new Parser<>(getfile("UltraFastParser/TstData2/data1_0.csv"), parserHandlersProducer.handlers, () -> new Tick());

        Instant dt0 = LocalDateTime.of(2011, 11, 21, 2, 0, 8, 700_000_000).atZone(parserHandlersProducer.zoneId).toInstant();
        Instant dt1 = LocalDateTime.of(2012, 9, 26, 3, 55, 21, 222_000_000).atZone(parserHandlersProducer.zoneId).toInstant();

        Assert.assertEquals(parser.startTime(), dt0);
        Assert.assertEquals(parser.endTime(), dt1);
    }


    @Test
    public void TestUltraFastSingleCsvParserStartEndTimesSingleLine() {
        ParserHandlersProducer parserHandlersProducer = getStdNyTickSettings();

        Parser<Tick> parser = new Parser<>(getfile("UltraFastParser/TstData2/data2_0.csv"), parserHandlersProducer.handlers, () -> new Tick());

        Instant dt0 = LocalDateTime.of(2011, 11, 21, 2, 0, 8, 700_000_000).atZone(parserHandlersProducer.zoneId).toInstant();

        Assert.assertEquals(parser.startTime(), dt0);
        Assert.assertEquals(parser.endTime(), dt0);
    }

    @Test
    public void TestUltraFastSingleCsvParser_3() {
        //
        // Test that two parsers are able to open the same .csv file simultaneously.
        //

        ParserHandlersProducer parserHandlersProducer = getStdNyTickSettings();

        Parser<Tick> parser = new Parser<>(getfile("UltraFastParser/TstData2/data2_0.csv"), parserHandlersProducer.handlers, () -> new Tick());

        Parser<Tick> parser1 = new Parser<>(getfile("UltraFastParser/TstData2/data2_0.csv"), parserHandlersProducer.handlers, () -> new Tick());

        Instant dt0 = LocalDateTime.of(2011, 11, 21, 2, 0, 8, 700_000_000).atZone(parserHandlersProducer.zoneId).toInstant();

        Assert.assertEquals(parser.startTime(), dt0);
        Assert.assertEquals(parser.endTime(), dt0);

        Assert.assertEquals(parser1.startTime(), dt0);
        Assert.assertEquals(parser1.endTime(), dt0);


    }

    @Test
    public void TestUltraFastSingleCsvParser_IQFEED_6() {
        //
        // Check that IQFEED format parsing works.
        //

        ParserHandlersProducer parserHandlersProducer = getStdNyTickSettings();

        Parser<Tick> parser = new Parser<>(getfile("UltraFastParser/TstData2/data2_0.csv"), parserHandlersProducer.handlers, () -> new Tick());



        Assert.assertTrue(parser.seek(parser.startTime()));

        Instant dt0 = LocalDateTime.of(2011, 11, 21, 2, 0, 8, 700_000_000).atZone(parserHandlersProducer.zoneId).toInstant();

        Assert.assertEquals(parser.current().DtGmt(), dt0);
    }


    Instant getTime(int y, int month, int d, int h, int m, int s, int mil, ZoneId zoneId){
        return LocalDateTime.of(y, month, d, h, m, s, mil*1000_000).atZone(zoneId).toInstant();
    }

    @Test
    public void TestUltraFastSingleCsvParser_FORTS_FUT()
    {

        ParserHandlersProducer parserHandlersProducer = getStdFortsTickSettings();


        Parser<Tick> parser = new Parser<>(getfile("UltraFastParser/Seek2/RI#_0.csv"), parserHandlersProducer.handlers, () -> new Tick());

        Assert.assertTrue(parser.seek(parser.startTime()));


        Assert.assertEquals(parser.current().DtGmt(), getTime(2012, 1, 3, 10, 0, 0, 53, parserHandlersProducer.zoneId));
        Assert.assertEquals(parser.current().last(), 137495,0.00001);
        Assert.assertEquals(parser.current().vol(), 1);
//        Assert.assertEquals(parser.CurrentQuote().Ask(), 0,0.00001);
  //      Assert.assertEquals(parser.CurrentQuote().Bid(), 0,0.00001);
        //FIXME Assert.assertEquals(parser.CurrentQuote().CumVol, 0);
        Assert.assertEquals(parser.current().getTickNumber(), 483738513l);

        Assert.assertTrue(parser.read());


        Assert.assertEquals(parser.current().DtGmt(), getTime(2012, 1, 3, 10, 0, 0, 53, parserHandlersProducer.zoneId));
        Assert.assertEquals(parser.current().last(), 137500,0.00001);
        Assert.assertEquals(parser.current().vol(), 1);
    //    Assert.assertEquals(parser.CurrentQuote().Ask(), 0,0.00001);
     //   Assert.assertEquals(parser.CurrentQuote().Bid(), 0,0.00001);
        //FIXME Assert.assertEquals(parser.CurrentQuote().CumVolume, 0);
        Assert.assertEquals(parser.current().tickNumber(), 483738514l);
    }

    @Test
    public void testLongFileTicks() {
        ParserHandlersProducer parserHandlersProducer = new ParserHandlersProducer(new CommonIniSettings().loadFromFile(getfile("LongFile/common.ini")));
        Parser<Tick> parser = new Parser<>(getfile("LongFile/XG_#.csv"), parserHandlersProducer.handlers, () -> new Tick());
        int cnt = 0;
        while (parser.read()){
            cnt+=1;
        }
        Assert.assertEquals(345600,cnt);
    }

    @Test
    public void testSearch() {
        ParserHandlersProducer parserHandlersProducer = new ParserHandlersProducer(new CommonIniSettings().loadFromFile(getfile("LongFile/common.ini")));
        Parser<Tick> parser = new Parser<>(getfile("LongFile/XG_#.csv"), parserHandlersProducer.handlers, () -> new Tick());

        Instant instant = dateUtils.parseAtZone("11.03.2013 23:59:59", dateUtils.nyZoneId());
        parser.seek(instant);
        Assert.assertEquals(instant,parser.current().dtGmt());

    }


    @Test
    public void testDukasFileTicks() {
        ParserHandlersProducer parserHandlersProducer = new ParserHandlersProducer(new CommonIniSettings().loadFromFile(getfile("dukas/common.ini")));

        Parser<Tick> parser = new Parser<>(getfile("dukas/audnzd.csv"), parserHandlersProducer.handlers, () -> new Tick());
        int cnt = 0;
        while (parser.read()){
            cnt+=1;
        }
        Assert.assertEquals(1000,cnt);
    }




}
