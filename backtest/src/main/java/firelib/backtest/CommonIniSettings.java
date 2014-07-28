package firelib.backtest;

public class CommonIniSettings
{
    public String[] COLUMNFORMAT; // list of tokens: 'D', 'T', '#', 'P', 'V', 'I'.
    public String DATEFORMAT;
    public String NEXTFILE; // ParseAndMergeCommonIni() expanthis this to absolute path!
    public String PREVFILE; // ParseAndMergeCommonIni() expanthis this to absolute path!

    public String TIMEFORMAT;
    public String TIMEZONE;
}
