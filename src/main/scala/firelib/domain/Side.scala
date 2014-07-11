package firelib.domain

object Side extends Enumeration {
  type Side = Value
  val Buy = Side(1)
  val Sell = Side(-1)
  val None = Side(0)

  def Opposite: Side = {
    if (this == None) {
      return None;
    }
    return if (this == Buy) Sell else Buy;
  }

  def SignForSide: Int = {
    return this.Value.id;
  }


  def SideForAmt(amt: Int): Side = {
    if (amt == 0) return None;
    return if (amt > 0) Buy else Sell;
  }

}


