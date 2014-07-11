package firelib.domain

object OrderStatus extends Enumeration {
  type OrderStatus = Value
  //happens right after id assigned by framework
  val New,
  //accepted by market
  Accepted,
  PendingCancel,
  CancelFailed,
  Done,
  Rejected,
  Cancelled = Value

  def IsFinal: Boolean = {
    return this == Rejected || this == Done || this == Cancelled;
  }

  def IsPending: Boolean = {
    return this == New || this == PendingCancel;
  }

  def IsLiveAccepted: Boolean = {
    return this == Accepted;
  }

}


