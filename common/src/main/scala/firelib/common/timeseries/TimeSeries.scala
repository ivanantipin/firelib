package firelib.common.timeseries

import firelib.common.misc.SubTopic

trait TimeSeries[T] {

    def count : Int

    def apply(idx: Int): T

    val onNewBar : SubTopic[TimeSeries[T]]

}
