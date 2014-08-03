package firelib.parser;

import java.io.FileInputStream;
import java.util.Properties;

public class CommonIniSettings {
    public String[] COLUMNFORMAT; // list of tokens: 'D', 'T', '#', 'P', 'V', 'I'.
    public String DATEFORMAT;
    public String NEXTFILE; // ParseAndMergeCommonIni() expanthis this to absolute path!
    public String PREVFILE; // ParseAndMergeCommonIni() expanthis this to absolute path!

    public String TIMEFORMAT;
    public String TIMEZONE;

    /*
[CHAIN]
PREVFILE=\201212\

[FORMATS]
DATEFORMAT=DD.MM.YYYY
TIMEFORMAT=HHMMSS
COLUMNFORMAT=D_T_#_P_V_U_B_A_I
TIMEZONE=NY

     */

    public CommonIniSettings initFromFile(String fn) {

        try (FileInputStream inStream = new FileInputStream(fn)) {
            Properties properties = new Properties();
            properties.load(inStream);
            DATEFORMAT = properties.getProperty("DATEFORMAT");
            TIMEFORMAT = properties.getProperty("TIMEFORMAT");
            TIMEZONE = properties.getProperty("TIMEZONE");

            COLUMNFORMAT = properties.getProperty("COLUMNFORMAT").split("_");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

}
