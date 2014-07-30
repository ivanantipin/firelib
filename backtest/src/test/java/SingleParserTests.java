import firelib.common.Tick;
import firelib.parser.CommonIniSettings;
import firelib.parser.Parser;
import firelib.parser.TokenGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Created by ivan on 7/30/14.
 */
public class SingleParserTests {

    String getfile(String name){
        URL resource = this.getClass().getClassLoader().getResource(name);
        return resource.getFile();
    }

    @Test
    public void TestUltraFastSingleCsvParser_1()
    {
        CommonIniSettings commonIniSettings = new CommonIniSettings();
        commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
        commonIniSettings.TIMEFORMAT = "HHMMSS";
        commonIniSettings.COLUMNFORMAT = new String[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"};
        commonIniSettings.TIMEZONE = "NY";

        TokenGenerator tokenGenerator = new TokenGenerator(commonIniSettings);
        Parser<Tick> parser = new Parser<>(getfile("UltraFastParser/TstData2/data1_0.csv"), tokenGenerator.handlers, ()->new Tick());



        Instant dt0 = LocalDateTime.of(2011, 11, 21, 2, 0, 8, 700_000_000).atZone(tokenGenerator.zoneId).toInstant();
        Instant dt1 = LocalDateTime.of(2012, 9, 26, 3, 55, 21, 222_000_000).atZone(tokenGenerator.zoneId).toInstant();

        Assert.assertEquals(parser.StartTime(), dt0);
        Assert.assertEquals(parser.EndTime(), dt1);
    }

}
