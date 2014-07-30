package firelib.common


object Side {
    val Sell = new Side("Sell", -1);
    val Buy = new Side("Buy", 1);
    val None = new Side("None", -1);

    def SideForAmt(amt: Int): Side = {
        if (amt == 0) return None
        if (amt > 0) Buy else Sell
    }

}

sealed class Side private(val Name: String, val sign: Int) {

    def Opposite: Side = {
        if (this == Side.None) {
            return Side.None;
        }
        if (this == Side.Buy) Side.Sell else Side.Buy;
    }

    override def toString: String = Name

}

