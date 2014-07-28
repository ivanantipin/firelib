package firelib.common;


import java.time.Instant;

public class Ohlc {
    public double C;
    public Instant DtGmtEnd;
    public double H = Integer.MIN_VALUE;
    public double L = Integer.MAX_VALUE;
    public double O = Double.NaN;
    public int Oi;
    public int Volume;

    public Ohlc() {

    }

    public Ohlc(Ohlc other) {
        InitFrom(other);
        DtGmtEnd = other.DtGmtEnd;
    }

    private void InitFrom(Ohlc other) {
        O = other.O;
        H = other.H;
        L = other.L;
        C = other.C;
        Volume = other.Volume;
        Oi = other.Oi;
    }

    private void InitFrom(Tick other) {
        O = other.Last;
        H = other.Last;
        L = other.Last;
        C = other.Last;
        Volume = other.Vol;
        Oi = 0;
    }

    public boolean Interpolated() {
        return Volume == 0;
    }

    private void AddPrice(double last) {
        if (Double.isNaN(O))
            O = last;

        if (H < last) {
            H = last;
        }
        if (L > last) {
            L = last;
        }
        C = last;
    }

    public void AddTick(Tick tick) {
        if (Interpolated()) {
            InitFrom(tick);
        }
        AddPrice(tick.Last);
        Volume += tick.Vol;
    }

    public void AddOhlc(Ohlc ohlc) {
        if (ohlc.Interpolated()) {
            return;
        }
        if (Interpolated()) {
            InitFrom(ohlc);
        }
        if (Double.isNaN(O))
            O = ohlc.O;

        if (H < ohlc.H)
            H = ohlc.H;

        if (L > ohlc.L)
            L = ohlc.L;
        C = ohlc.C;
        Volume += ohlc.Volume;
        Oi = ohlc.Oi;
    }

    public double nprice() {
        return (C + H + L) / 3;
    }

    public double medium() {
        return (H + L) / 2;
    }

    public boolean IsUpBar() {
        return C > O;
    }

    public double Range() {
        return H - L;
    }

    public double UpShadow() {
        return H - C;
    }

    public double DownShadow() {
        return C - L;
    }

    public double BodyLength() {
        return Math.abs(C - O);
    }

    public double Return() {
        return C - O;
    }

    public boolean InRange(double val) {
        return H > val && val > L;
    }


    public String toString() {
        return String.format("OHLC({0}/{1}/{2}/{3}@/{4}/{5})", O, H, L, C, DtGmtEnd.toString(), Interpolated());
    }

    public void Interpolate(Ohlc prev) {
        InitFrom(prev);
        Volume = 0;
    }
}
