package firelib.common

import java.time.Instant

import scala.collection.mutable.ArrayBuffer


class Order(val orderType: OrderType, val price: Double, val qty: Int, val side: Side, val security : String, val id: String) {
    
    assert(qty > 0, "order qty must be > 0!!")
    assert(price > 0 || orderType == OrderType.Market, "price must be > 0!!")
    assert(id != null, "id must be not null!!")
    assert(security != null, "security must be not null!!")

    var placementTime: Instant  = _
    var reason: String = _
    def status = statuses.last
    val trades = new ArrayBuffer[Trade]()
    val statuses = new ArrayBuffer[OrderStatus]()

    def remainingQty : Int = qty - trades.map(_.qty).sum

    override def toString: String = s"Order(price=$price qty=$qty side=$side type=$orderType orderId=$id sec=$security)"
}
