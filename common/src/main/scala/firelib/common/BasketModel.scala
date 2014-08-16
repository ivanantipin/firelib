package firelib.common

import java.time.Instant

import firelib.domain.Ohlc

import scala.collection.mutable.ArrayBuffer



abstract class BasketModel extends IModel {
    private var modelProperties: Map[String, String] = _
    var mdDistributor: IMarketDataDistributor = _
    var dtGmt: Instant  = _
    var marketStubs: Array[IMarketStub] =_

    override def properties: Map[String, String] = modelProperties

    override def name: String = getClass.getName

    override def stubs: Seq[IMarketStub] = marketStubs

    override def trades: Seq[Trade] = marketStubs.flatMap(_.trades)

    override def hasValidProps() = true

    protected def flattenAll(reason: Option[String] = None) = marketStubs.foreach(_.flattenAll(reason))

    protected def cancelAllOrders = marketStubs.foreach(_.cancelAllOrders)


    override def initModel(modelProps: Map[String, String], mktStubs: Seq[IMarketStub], ctx: IMarketDataDistributor) = {
        mdDistributor = ctx
        marketStubs = mktStubs.toArray
        modelProperties = modelProps
        applyProperties(modelProps)
    }

    def enableOhlcHistory(intr: Interval, lengthToMaintain: Int = -1): ArrayBuffer[ITimeSeries[Ohlc]] = {
        var rt = new ArrayBuffer[ITimeSeries[Ohlc]]()
        for (i <- 0 until marketStubs.length) {
            rt += mdDistributor.activateOhlcTimeSeries(i, intr, lengthToMaintain)
        }
        return rt
    }

    def buyAtLimit(price: Double, vol: Int = 1, idx: Int = 0) = {
        marketStubs(idx).submitOrders(List(new Order(OrderType.Limit, price, vol, Side.Buy)))
    }

    def sellAtLimit(price: Double, vol: Int = 1, idx: Int = 0) = {
        marketStubs(idx).submitOrders(List(new Order(OrderType.Limit, price, vol, Side.Sell)))
    }

    def buyAtStop(price: Double, vol: Int = 1, idx: Int = 0) = {
        marketStubs(idx).submitOrders(List(new Order(OrderType.Stop, price, vol, Side.Buy)))
    }

    def sellAtStop(price: Double, vol: Int = 1, idx: Int = 0) = {
        marketStubs(idx).submitOrders(List(new Order(OrderType.Stop, price, vol, Side.Sell)))
    }



    def getTs(mdt: Interval, idx: Int = 0): ITimeSeries[Ohlc] = {
        return mdDistributor.activateOhlcTimeSeries(idx, mdt, -1)
    }


    def getOrderForDiff(currentPosition: Int, targetPos: Int): Option[Order] = {
        val vol = targetPos - currentPosition
        if (vol != 0) {
            return Some(new Order(OrderType.Market, 0, math.abs(vol), if (vol > 0) Side.Buy else Side.Sell))
        }
        return None
    }

    /*
     * override this method for proper logging
     */

    protected def log(message: String) = {

    }


    protected def managePosTo(pos: Int, idx: Int = 0): Unit = {
        getOrderForDiff(marketStubs(idx).position, pos) match {
            case Some(ord) => marketStubs(idx).submitOrders(List(ord))
            case _ =>
        }
    }

    protected def applyProperties(mprops: Map[String, String])

    protected def onIntervalEnd(dtGmt:Instant) = {}

    protected def position(idx: Int = 0) = marketStubs(idx).position

    def onBacktestEnd()


    def onStep(dtGmt:Instant) = {
        this.dtGmt = dtGmt
        onIntervalEnd(dtGmt)
    }
}
