package firelib.indicators
import firelib.common.Side

class SlippageLoss(val orderThresholds: IndexedSeq[Int]) {
    val vals = Array.fill(orderThresholds.length)(0.0)

    def apply(idx: Int): Double = vals(idx)

    def slotForQty(qty: Int): Int = {
        var idx = 0
        while (idx < orderThresholds.length && qty >= orderThresholds(idx)){
            idx+=1
        }
        idx - 1
    }


    def add(orderInfo: OrderInfo) = upd(orderInfo, 1)

    def remove(orderInfo: OrderInfo) = upd(orderInfo, -1)

    def upd(orderInfo: OrderInfo, sign: Int) {
        val cat = slotForQty(orderInfo.qty);
        if (orderInfo.side == Side.Buy) {
            vals(cat) += sign * orderInfo.qty * (orderInfo.vwap - orderInfo.minPrice);
        }
        else {
            vals(cat) += sign * orderInfo.qty * (orderInfo.maxPrice - orderInfo.vwap);
        }
    }
}
