package firelib.domain

import org.joda.time.DateTime
import firelib.domain.OrderType.OrderType
import firelib.domain.Side.Side
import firelib.domain.OrderStatus.OrderStatus

class Order(val OrderType: OrderType, val Price: Double, val Qty: Int, val OrderSide: Side) {

  if (Qty <= 0) {
    throw new Exception("amount can't be negative or zero " + Qty);
  }

  var MinutesToHold = -1;
  var Id: String = _
  var Security: String = _
  var Reason: String = _

  var PlacementTime: DateTime = _
  var ValidUntil: DateTime = _
  var Status: OrderStatus = _


  override def toString: String = {
    return "Order(%s@%s/%s/%s/Id:%s/%s)" format(Price, Qty, Side, OrderType, Id, Security);
  }
}
