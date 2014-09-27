package firelib.parser;

import firelib.domain.Ohlc;
import firelib.domain.Tick;
import javolution.text.TypeFormat;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ParserHandlersProducer {

    private CommonIniSettings commonIniSettings;

    public boolean isOhlc(){
        return new ArrayList<String>(Arrays.asList(commonIniSettings.COLUMNFORMAT)).contains("O");
    }

    private ZoneId getZone(){
        switch (commonIniSettings.TIMEZONE){
            case "NY":
                return ZoneId.of("America/New_York");
            case "MOSCOW":
                return ZoneId.of("Europe/Moscow");
            case "LONDON":
                return ZoneId.of("Europe/London");

        }
        return ZoneId.systemDefault();
    }

    public final ZoneId zoneId;
    public final IHandler[] handlers;

    private final String dateformat;


    public ParserHandlersProducer(CommonIniSettings commonIniSettings){
        this.commonIniSettings = commonIniSettings;
        zoneId = getZone();
        dateformat = getDateFormat(commonIniSettings);
        handlers = parsePattern();
    }

    public static ParserHandlersProducer loadFromFile(String file){
        return new ParserHandlersProducer(new CommonIniSettings().loadFromFile(file));
    }

    private boolean isMillisSymbol(CommonIniSettings commonIniSettings, int i) {
        return commonIniSettings.COLUMNFORMAT[i].equals("#") && commonIniSettings.COLUMNFORMAT[i - 1].equals("T") && commonIniSettings.COLUMNFORMAT[i - 2].equals("D");
    }


    /**
     * this is bloody hardcoded datetime formats resolution
     * @param settings
     * @return
     */
    private String getDateFormat(CommonIniSettings settings){
        if(settings.DATEFORMAT.equals("DD.MM.YYYY") && settings.TIMEFORMAT.equals("HHMMSS")){

            if(isMillisSymbol(settings,2)){
                return "dd.MM.yyyy,HHmmss.SSS";
            }
            return "dd.MM.yyyy,HHmmss";

        }

        if(settings.DATEFORMAT.equals("YYYY-MM-DD") && settings.TIMEFORMAT.equals("HH:MM:SS")){
            if(isMillisSymbol(settings,2)){
                return "yyyy-MM-dd HH:mm:ss.SSS";
            }
            return "yyyy-MM-dd HH:mm:ss";
        }
        throw new RuntimeException("not supported");
    }


    private IHandler[] parsePattern()
    {
        List<IHandler> microcode = new ArrayList<>();

        for (int i = 0; i < commonIniSettings.COLUMNFORMAT.length; i++)
        {
            String token = commonIniSettings.COLUMNFORMAT[i];

            switch (token)
            {
                case "D":
                    if(isOhlc()){
                        microcode.add(new DateTimeHandler<Ohlc>((oh,dt)->oh.setDtGmtEnd(dt), zoneId, dateformat));
                    }else{
                        microcode.add(new DateTimeHandler<Tick>((oh,dt)->oh.setDtGmt(dt), zoneId, dateformat));
                    }
                    break;

                case "T":
                    break;
                case "#":
                    if(!isMillisSymbol(commonIniSettings, i)){
                        microcode.add(new SkipHandler());
                    }
                    // This special field just mean microseconds are present in time.
                    // Just skip it.
                    break;

                case "P":
                    microcode.add(new StdHandler<Tick,Double>((oh,v)->oh.setLast(v), (chs)-> TypeFormat.parseDouble(chs)));
                    break;

                case "O":
                    microcode.add(new StdHandler<Ohlc,Double>((oh,v)->oh.setO(v), (chs)->TypeFormat.parseDouble(chs)));
                    break;

                case "H":
                    microcode.add(new StdHandler<Ohlc,Double>((oh,v)->oh.setH(v), (chs)->TypeFormat.parseDouble(chs)));
                    break;

                case "L":
                    microcode.add(new StdHandler<Ohlc,Double>((oh,v)->oh.setL(v), (chs)->TypeFormat.parseDouble(chs)));
                    break;

                case "C":
                    microcode.add(new StdHandler<Ohlc,Double>((oh,v)->oh.setC(v), (chs)->TypeFormat.parseDouble(chs)));
                    break;

                case "V":
                    if(isOhlc()){
                        microcode.add(new StdHandler<Ohlc,Integer>((oh,v)->oh.setVolume(v), (chs)->TypeFormat.parseInt(chs)));
                    }else{
                        microcode.add(new StdHandler<Tick,Integer>((oh,v)->oh.setVol(v), (chs)->(int)TypeFormat.parseDouble(chs)));
                    }

                    break;
                case "I":
                    microcode.add(new StdHandler<Tick,Integer>((oh,v)->oh.setTickNumber(v), (chs)->TypeFormat.parseInt(chs)));
                    break;
                case "U":
                    microcode.add(new SkipHandler());
                    //cum volume
                    break;
                case "B":
                    microcode.add(new StdHandler<Tick,Double>((oh,v)->oh.setBid(v), (chs)->TypeFormat.parseDouble(chs)));
                    break;

                case "A":
                    microcode.add(new StdHandler<Tick,Double>((oh,v)->oh.setAsk(v), (chs)->TypeFormat.parseDouble(chs)));
                    break;
                default:
                    throw new RuntimeException("Error: unsupported COLUMNFORMAT token '" + token + "'.");
            }
        }
        List<IHandler> withIncs = new ArrayList<>();
        for(int i = 0; i < microcode.size(); i++){
            withIncs.add(microcode.get(i));
            if(i != microcode.size() -1){
                withIncs.add(new IncHandler<>());
            }
        }
        return withIncs.toArray(new IHandler[0]);
    }

}
