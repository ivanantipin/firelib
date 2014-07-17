package firelib.backtest

import firelib.domain.OptimizedParameter

import scala.collection.mutable


class ParamsVariator(optParams: Seq[OptimizedParameter]) {

    val optimizationParams = optParams.map(op => op.GetVariations)
    var idx = 0;
    val dividers = new Array[Int](optimizationParams.length)
    var Combinations = 1;
    for (i <- optimizationParams.length - 1 to 0 by -1) {
        dividers(i) = Combinations;
        Combinations *= optimizationParams(i).length;
    }

    def Next: Map[String, Int] = {
        if (idx >= Combinations) {
            return null;
        }
        var ret = new mutable.HashMap[String, Int]();
        var idxTmp = idx;
        for (i <- 0 to dividers.length) {
            ret += optParams(i).Name -> optimizationParams(i)(idxTmp / dividers(i));
            idxTmp %= dividers(i)
        }
        idx += 1
        return ret.toMap;
    }

}
