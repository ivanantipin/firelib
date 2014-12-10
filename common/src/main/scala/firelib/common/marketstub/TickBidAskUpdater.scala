package firelib.common.marketstub

import firelib.domain.Tick

class TickBidAskUpdater(val updatee : (Double,  Double)=>Unit) extends (Tick=>Unit){

    override def apply(tt: Tick): Unit = {
        if(!tt.bid.isNaN){
            updatee(tt.bid,tt.ask)
        }
    }
}
