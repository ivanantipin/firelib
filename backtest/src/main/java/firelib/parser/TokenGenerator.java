package firelib.parser;

import firelib.common.Ohlc;
import firelib.common.Tick;
import javolution.text.TypeFormat;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ivan on 7/27/14.
 */
class TokenGenerator{


    boolean isOhlc(CommonIniSettings commonIniSettings){
        return false;
    }

    ZoneId getZone(CommonIniSettings commonIniSettings){
        return null;

    }



    public IHandler[] parsePattern(CommonIniSettings commonIniSettings)
    {
        List<IHandler> microcode = new ArrayList<>();

        for (int i = 0; i < commonIniSettings.COLUMNFORMAT.length; i++)
        {
            String token = commonIniSettings.COLUMNFORMAT[i];

/*
FIXME
            if (microcode.Count > 0 &&  !IsMillisSymbol(commonIniSettings,i))
            {
                microcode.Add(new Inc());
            }
*/

            switch (token)
            {
                case "D":
                    if(isOhlc(commonIniSettings)){
                        microcode.add(new DateTimeHandler<Ohlc>((oh,dt)->oh.setDtGmtEnd(dt), getZone(commonIniSettings)));
                    }else{
                        microcode.add(new DateTimeHandler<Tick>((oh,dt)->oh.setDtGmt(dt), getZone(commonIniSettings)));
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
                    microcode.add(new StdHandler<Ohlc,Integer>((oh,v)->oh.setVolume(v), (chs)->TypeFormat.parseInt(chs)));
                    break;
                case "I":
                    microcode.add(new SkipHandler());
                    //trade id sequence
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

        //FIXME microcode.Add(new SkipTillEndLine());
        //FIXME microcode.Add(new SkipEndLine());
        return microcode.toArray(new IHandler[0]);
    }

    private boolean isMillisSymbol(CommonIniSettings commonIniSettings, int i) {
        return commonIniSettings.COLUMNFORMAT[i - 1].equals("T") && commonIniSettings.COLUMNFORMAT[i - 2].equals("D");
    }
}
