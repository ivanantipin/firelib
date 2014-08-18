package firelib.indicators

class SimpleMovingAverage(val period: Int, val calcSko: Boolean) {


    private val closes = Array.fill[Double](period)(0)

    private var currentSko = 0.0
    private var currentSma = 0.0
    var enabled = false
    private var pos = 0


    def value = currentSma / (if (enabled) period else pos)

    def sko = currentSko


    def add(cc: Double): Unit = {
        if (cc.isNaN) {
            return
        }
        currentSma -= closes(pos)
        currentSma += cc
        closes(pos) = cc
        pos += 1
        if (pos == closes.length) {
            pos = 0;
            enabled = true;
        }

        if (calcSko) {
            var ssma = value;
            var per = if (enabled) pos else period
            var sig = 0.0;
            for (i <- 0 until per) {
                var cl = closes(i);
                sig += (cl - ssma) * (cl - ssma);
            }
            sig /= per;
            currentSko = math.pow(sig, 0.5);
        }
    }

}
