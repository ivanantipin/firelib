package firelib.common

import org.joda.time.DateTime

class Ohlc extends Timed {


    def Ohlc(other: Ohlc) {
        InitFrom(other);
        DtGmtEnd = other.DtGmtEnd;
    }

    def Ohlc() {

    }

    var C: Double = Double.NaN;
    var DtGmtEnd: DateTime = _;
    var H = Double.MinValue;
    var L = Double.MaxValue;
    var O = Double.NaN;
    var Oi: Int = 0
    var Volume: Int = 0


    def InitFrom(other: Ohlc) = {
        O = other.O;
        H = other.H;
        L = other.L;
        C = other.C;
        Volume = other.Volume;
        Oi = other.Oi;
    }


    def InitFrom(other: Tick) = {
        O = other.last;
        H = other.last;
        L = other.last;
        C = other.last;
        Volume = other.Vol;
        Oi = 0;
    }


    def Interpolated: Boolean = {
        return Volume == 0
    }


    def AddPrice(last: Double) = {
        if (O.isNaN)
            O = last;

        if (H < last) {
            H = last;
        }
        if (L > last) {
            L = last;
        }
        C = last;
    }

    def AddTick(tick: Tick) = {
        if (Interpolated) {
            InitFrom(tick);
        }
        AddPrice(tick.last);
        Volume += tick.Vol;
    }

    def AddOhlc(ohlc: Ohlc) {
        if (ohlc.Interpolated) {
            return;
        }
        if (Interpolated) {
            InitFrom(ohlc);
        }
        if (O.isNaN)
            O = ohlc.O;

        if (H < ohlc.H)
            H = ohlc.H;

        if (L > ohlc.L)
            L = ohlc.L;
        C = ohlc.C;
        Volume += ohlc.Volume;
        Oi = ohlc.Oi;
    }


    def nprice: Double = {
        return (C + H + L) / 3;
    }

    def medium: Double = {
        return (H + L) / 2;
    }

    def IsUpBar: Boolean = {
        return C > O;
    }

    def Range: Double = {
        return H - L
    }

    def UpShadow: Double = {
        return H - C
    }

    def DownShadow = {
        C - L
    }

    def BodyLength = {
        math.abs(C - O)
    }

    def Return = {
        C - O
    }

    def InRange(vall: Double) = {
        H > vall && vall > L
    }


    /*
            public override string ToString()
            {
                return string.Format("OHLC({0}/{1}/{2}/{3}@/{4}/{5})", O, H, L, C, DtGmtEnd.ToString("dd.MM.yyyy HH:mm:ss "), Interpolated);
            }
    */

    def Interpolate(prev: Ohlc) = {
        InitFrom(prev);
        Volume = 0;
    }

    override def DtGmt: DateTime = DtGmtEnd
}
