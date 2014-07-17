import firelib.backtest.{IMarketDataDistributor, IMarketStub, IModel}
import firelib.domain._
import org.joda.time.DateTime

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

abstract class BasketModel extends IModel {
    var ModelProperties: Map[String, String] = _
    var mdDistributor: IMarketDataDistributor = _
    var DtGmt: DateTime = _
    var stubs: Array[IMarketStub] = _

    def InitModel(modelProps: Map[String, String], marketStubs: Array[IMarketStub], ctx: IMarketDataDistributor) = {
        mdDistributor = ctx;
        stubs = marketStubs;
        ModelProperties = modelProps;
        ApplyProperties(modelProps);
    }

    def BuyAtLimit(price: Double, vol: Int = 1, idx: Int = 0) = {
        stubs(idx).SubmitOrders(new Order(OrderTypeEnum.Limit, price, vol, SideEnum.Buy));
    }

    def SellAtLimit(price: Double, vol: Int = 1, idx: Int = 0) = {
        stubs(idx).SubmitOrders(new Order(OrderTypeEnum.Limit, price, vol, SideEnum.Sell));
    }

    def BuyAtStop(price: Double, vol: Int = 1, idx: Int = 0) = {
        stubs(idx).SubmitOrders(new Order(OrderTypeEnum.Stop, price, vol, SideEnum.Buy));
    }

    def SellAtStop(price: Double, vol: Int = 1, idx: Int = 0) = {
        stubs(idx).SubmitOrders(new Order(OrderTypeEnum.Stop, price, vol, SideEnum.Sell));
    }

    def EnableOhlcHistory(intr: Interval, lengthToMaintain: Int = -1): ArrayBuffer[ITimeSeries[Ohlc]] = {
        var rt = new ArrayBuffer[ITimeSeries[Ohlc]]();
        for (i <- stubs.length) {
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
            return new Order(OrderTypeEnum.Market, 0, math.abs(vol), if (vol > 0) SideEnum.Buy else SideEnum.Sell);
        }
        return null
    }

    /*
     * override this method for proper logging
     */

    protected def Log(message: String) = {

    }


    protected def ManagePosTo(pos: Int, idx: Int = 0): Unit = {
        if (stubs(idx).Position != stubs(idx).UnconfirmedPosition) {
            Log(String.format("Unconfirmed position %s not equals to position %s ignoring managing position to " + pos, stubs(idx).Position, stubs(idx).UnconfirmedPosition));
            return;
        }
        var ord = GetOrderForDiff(stubs(idx).Position, pos);
        if (ord != null) {
            stubs(idx).SubmitOrders(ord);
        }
    }

    protected abstract def ApplyProperties(mprops: Map[String, String])

    protected def OnIntervalEnd(dtGmt: DateTime) = {}

    protected def Position(idx: Int = 0) = stubs(idx).Position

    def trades: ListBuffer[Trade] = {
        var ret = new ListBuffer[Trade]()
        stubs.foreach(t => ret ++= t.trades)
        return ret;
    }


    def onBacktestEnd


    def name = {
        getClass.getName
    }

    def hasValidProps = true


    protected def FlattenAll(reason: String = null) = {
        stubs.foreach(t => t.FlattenAll(reason));
    }

    protected def CancelAllOrders = {
        stubs.foreach(t => t.CancelOrders)
    }

    def OnStep(dtGmt: DateTime) = {
        DtGmt = dtGmt;
        OnIntervalEnd(dtGmt);
    }
}
