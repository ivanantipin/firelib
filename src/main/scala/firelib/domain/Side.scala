package firelib.domain


object SideEnum {
    val Sell = new Side("Sell", -1);
    val Buy = new Side("Buy", 1);
    val None = new Side("None", -1);

    def SideForAmt(amt: Int): Side = {
        if (amt == 0) return None
        if (amt > 0) Buy else Sell
    }

}

sealed class Side(val Name: String, val sign: Int) {

    def Opposite: Side = {
        if (this == SideEnum.None) {
            return SideEnum.None;
        }
        if (this == SideEnum.Buy) SideEnum.Sell else SideEnum.Buy;
    }

    override def toString: String = Name

}

