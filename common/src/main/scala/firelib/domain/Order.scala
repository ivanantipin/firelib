package firelib.common

import java.time.Instant


class Order(val orderType: OrderType, val price: Double, val qty: Int, val side: Side) {
    
    assert(qty > 0)
    
    var minutesToHold = -1
    var id: String = _
    var security: String = _
    var reason: String = _

    var placementTime: Instant  = _
    var validUntil: Instant  = _
    var status: OrderStatus = _

    override def toString: String = {
        return "Order(%s@%s/%s/%s/Id:%s/%s)" format(price, qty, side, orderType, id, security)
    }
}
