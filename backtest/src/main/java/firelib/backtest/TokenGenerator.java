package firelib.backtest;

import firelib.common.Ohlc;
import firelib.common.Tick;
import javolution.text.TypeFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ivan on 7/27/14.
 */
class TokenGenerator{
    public IHandler[] parsePattern(CommonIniSettings commonIniSettings)
    {
        List<IHandler> microcode = new ArrayList<>();

        for (int i = 0; i < commonIniSettings.COLUMNFORMAT.length; i++)
        {
            String token = commonIniSettings.COLUMNFORMAT[i];

            if (microcode.Count > 0 &&  !IsMillisSymbol(commonIniSettings,i))
            {
                microcode.Add(new Inc());
            }

            switch (token)
            {
                case "D":
                    //microcode.Add(GetDateHandler(commonIniSettings));
                    break;

                case "T":
                    //microcode.Add(GetTimeHandler(commonIniSettings));
                    break;

                case "#":
                    /*if (!IsMillisSymbol(commonIniSettings, i))
                    {
                        //skip symbol
                        microcode.Add(new SkipTillChar((byte) ','));
                    }*/
                    // This special field just mean microseconds are present in time.
                    // Just skip it.
                    break;

                case "P":
                    microcode.add(new StdParser<Tick,Double>((oh,v)->oh.Last = v, (chs)-> TypeFormat.parseDouble(chs)));
                    break;

                case "O":
                    microcode.add(new StdParser<Ohlc,Double>((oh,v)->oh.O = v, (chs)->TypeFormat.parseDouble(chs)));
                    break;

                case "H":
                    microcode.add(new StdParser<Ohlc,Double>((oh,v)->oh.H = v, (chs)->TypeFormat.parseDouble(chs)));
                    break;

                case "L":
                    microcode.add(new StdParser<Ohlc,Double>((oh,v)->oh.L = v, (chs)->TypeFormat.parseDouble(chs)));
                    break;

                case "C":
                    microcode.add(new StdParser<Ohlc,Double>((oh,v)->oh.C = v, (chs)->TypeFormat.parseDouble(chs)));
                    break;

                case "V":
                    microcode.add(new StdParser<Ohlc,Integer>((oh,v)->oh.Volume = v, (chs)->TypeFormat.parseInt(chs)));
                    break;

                case "I":
                    //trade id sequence
                    break;

                case "U":
                    //cum volume
                    break;

                case "B":
                    microcode.add(new StdParser<Tick,Double>((oh,v)->oh.Bid = v, (chs)->TypeFormat.parseDouble(chs)));
                    break;

                case "A":
                    microcode.add(new StdParser<Tick,Double>((oh,v)->oh.Ask = v, (chs)->TypeFormat.parseDouble(chs)));
                    break;
                default:
                    throw new RuntimeException("Error: unsupported COLUMNFORMAT token '" + token + "'.");
            }
        }

        microcode.Insert(0, new ResetHandler());
        microcode.Add(new SkipTillEndLine());
        microcode.Add(new SkipEndLine());
        return microcode.ToArray();
    }
}
