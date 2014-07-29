package firelib.common

import java.time.Instant

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

abstract class BasketModel extends IModel {
    var ModelProperties: Map[String, String] = _
    var mdDistributor: IMarketDataDistributor = _
    var DtGmt: Instant  = _
    var stubs: Array[IMarketStub] = _

    def InitModel(modelProps: Map[String, String], marketStubs: Array[IMarketStub], ctx: IMarketDataDistributor) = {
        mdDistributor = ctx;
        stubs = marketStubs;
        ModelProperties = modelProps;
        ApplyProperties(modelProps);
    }

    def BuyAtLimit(price: Double, vol: Int = 1, idx: Int = 0) = {
        stubs(idx).SubmitOrders(List(new Order(OrderType.Limit, price, vol, Side.Buy)));
    }

    def SellAtLimit(price: Double, vol: Int = 1, idx: Int = 0) = {
        stubs(idx).SubmitOrders(List(new Order(OrderType.Limit, price, vol, Side.Sell)));
    }

    def BuyAtStop(price: Double, vol: Int = 1, idx: Int = 0) = {
        stubs(idx).SubmitOrders(List(new Order(OrderType.Stop, price, vol, Side.Buy)));
    }

    def SellAtStop(price: Double, vol: Int = 1, idx: Int = 0) = {
        stubs(idx).SubmitOrders(List(new Order(OrderType.Stop, price, vol, Side.Sell)));
    }

    def EnableOhlcHistory(intr: Interval, lengthToMaintain: Int = -1): ArrayBuffer[ITimeSeries[Ohlc]] = {
        var rt = new ArrayBuffer[ITimeSeries[Ohlc]]();
        for (i <- 0 until stubs.length) {
            rt += mdDistributor.activateOhlcTimeSeries(i, intr, lengthToMaintain);
        }
        return rt;
    }


    def GetTs(mdt: Interval, idx: Int = 0): ITimeSeries[Ohlc] = {
        return mdDistributor.activateOhlcTimeSeries(idx, mdt, -1);
    }


    def GetOrderForDiff(currentPosition: Int, targetPos: Int): Order = {
        val vol = targetPos - currentPosition;
        if (vol != 0) {
            return new Order(OrderType.Market, 0, math.abs(vol), if (vol > 0) Side.Buy else Side.Sell);
        }
        return null
    }

    /*
     * override this method for proper logging
     */

    protected def Log(message: String) = {

    }


    protected def ManagePosTo(pos: Int, idx: Int = 0): Unit = {
        //if (stubs(idx).Position != stubs(idx).UnconfirmedPosition) {
        //    Log(String.format("Unconfirmed position %s not equals to position %s ignoring managing position to " + pos, stubs(idx).Position, stubs(idx).UnconfirmedPosition));
        //    return;
        //}
        var ord = GetOrderForDiff(stubs(idx).Position, pos);
        if (ord != null) {
            stubs(idx).SubmitOrders(List(ord));
        }
    }

    protected def ApplyProperties(mprops: Map[String, String])

    protected def OnIntervalEnd(dtGmt:Instant) = {}

    protected def Position(idx: Int = 0) = stubs(idx).Position

    def trades: ListBuffer[Trade] = {
        var ret = new ListBuffer[Trade]()
        stubs.foreach(ret ++= _.trades)
        return ret;
    }


    def onBacktestEnd


    val name = getClass.getName

    def hasValidProps = true


    protected def FlattenAll(reason: String = null) = stubs.foreach(_.FlattenAll(reason));

    protected def CancelAllOrders = stubs.foreach(_.CancelOrders)

    def OnStep(dtGmt:Instant) = {
        DtGmt = dtGmt;
        OnIntervalEnd(dtGmt);
    }
}
