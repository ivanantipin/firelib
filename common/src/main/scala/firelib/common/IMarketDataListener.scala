package firelib.common

/**
 * Created by ivan on 7/21/14.
 */
trait IMarketDataListener {
    def onOhlc(idx: Int, ohlc: Ohlc, next: Ohlc);
    def onTick(idx: Int, tick: Tick, next: Tick);

}
