package firelib.common

import java.time.Instant

import scala.collection.mutable.ArrayBuffer

abstract class BasketModel extends IModel {
    private var modelProperties: Map[String, String] = _
    var mdDistributor: IMarketDataDistributor = _
    var dtGmt: Instant  = _
    var marketStubs: Array[IMarketStub] = _

    override def initModel(modelProps: Map[String, String], mktStubs: Seq[IMarketStub], ctx: IMarketDataDistributor) = {
        mdDistributor = ctx;
        marketStubs = mktStubs.toArray;
        modelProperties = modelProps;
        applyProperties(modelProps);
    }

    def buyAtLimit(price: Double, vol: Int = 1, idx: Int = 0) = {
        marketStubs(idx).submitOrders(List(new Order(OrderType.Limit, price, vol, Side.Buy)));
    }

    def sellAtLimit(price: Double, vol: Int = 1, idx: Int = 0) = {
        marketStubs(idx).submitOrders(List(new Order(OrderType.Limit, price, vol, Side.Sell)));
    }

    def buyAtStop(price: Double, vol: Int = 1, idx: Int = 0) = {
        marketStubs(idx).submitOrders(List(new Order(OrderType.Stop, price, vol, Side.Buy)));
    }

    def sellAtStop(price: Double, vol: Int = 1, idx: Int = 0) = {
        marketStubs(idx).submitOrders(List(new Order(OrderType.Stop, price, vol, Side.Sell)));
    }

    def enableOhlcHistory(intr: Interval, lengthToMaintain: Int = -1): ArrayBuffer[ITimeSeries[Ohlc]] = {
        var rt = new ArrayBuffer[ITimeSeries[Ohlc]]();
        for (i <- 0 until marketStubs.length) {
            rt += mdDistributor.activateOhlcTimeSeries(i, intr, lengthToMaintain);
        }
        return rt;
    }


    def getTs(mdt: Interval, idx: Int = 0): ITimeSeries[Ohlc] = {
        return mdDistributor.activateOhlcTimeSeries(idx, mdt, -1);
    }


    def getOrderForDiff(currentPosition: Int, targetPos: Int): Order = {
        val vol = targetPos - currentPosition;
        if (vol != 0) {
            return new Order(OrderType.Market, 0, math.abs(vol), if (vol > 0) Side.Buy else Side.Sell);
        }
        return null
    }

    /*
     * override this method for proper logging
     */

    protected def log(message: String) = {

    }


    protected def managePosTo(pos: Int, idx: Int = 0): Unit = {
        //if (stubs(idx).Position != stubs(idx).UnconfirmedPosition) {
        //    Log(String.format("Unconfirmed position %s not equals to position %s ignoring managing position to " + pos, stubs(idx).Position, stubs(idx).UnconfirmedPosition));
        //    return;
        //}
        var ord = getOrderForDiff(marketStubs(idx).Position, pos);
        if (ord != null) {
            marketStubs(idx).submitOrders(List(ord));
        }
    }

    protected def applyProperties(mprops: Map[String, String])

    protected def onIntervalEnd(dtGmt:Instant) = {}

    protected def position(idx: Int = 0) = marketStubs(idx).Position

    def onBacktestEnd()

    override def properties: Map[String, String] = modelProperties

    override def name: String = getClass.getName

    override def stubs: Seq[IMarketStub] = marketStubs

    override def trades: Seq[Trade] = marketStubs.flatMap(_.trades)

    override def hasValidProps() = true


    protected def FlattenAll(reason: String = null) = marketStubs.foreach(_.flattenAll(reason));

    protected def CancelAllOrders = marketStubs.foreach(_.cancelOrders)

    def onStep(dtGmt:Instant) = {
        this.dtGmt = dtGmt;
        onIntervalEnd(dtGmt);
    }
}
