package firelib.common

import java.time.Instant


class Order(val OrdType: OrderType, val Price: Double, val Qty: Int, val OrderSide: Side) {

    if (Qty <= 0) {

        throw new Exception("amount can't be negative or zero " + Qty);
    }

    val dd = Direction.Down

    var MinutesToHold = -1;
    var Id: String = _
    var Security: String = _
    var Reason: String = _

    var PlacementTime: Instant  = _
    var ValidUntil: Instant  = _
    var Status: OrderStatus = _

    override def toString: String = {
        return "Order(%s@%s/%s/%s/Id:%s/%s)" format(Price, Qty, OrderSide, OrdType, Id, Security);
    }
}
