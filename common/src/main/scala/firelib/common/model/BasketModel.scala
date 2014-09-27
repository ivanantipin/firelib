package firelib.common.model

import java.time.Instant

import firelib.common.Trade
import firelib.common.interval.Interval
import firelib.common.marketstub.MarketStub
import firelib.common.mddistributor.MarketDataDistributor
import firelib.common.timeseries.TimeSeries
import firelib.domain.Ohlc

import scala.collection.immutable.IndexedSeq

/**
 * main base class for all strategies
 */
abstract class BasketModel extends Model with WithTradeUtils{

    /**
     * variable holds model properties
     */
    private var modelProperties: Map[String, String] = _

    protected var mdDistributor: MarketDataDistributor = _

    /**
     * this variable holds current time
     */
    protected var dtGmt: Instant  = Instant.MIN


    private var marketStubs: Array[MarketStub] =_

    override def properties: Map[String, String] = modelProperties

    override def name: String = getClass.getName

    override def stubs: Seq[MarketStub] = marketStubs

    override def trades: Seq[Trade] = marketStubs.flatMap(_.trades)

    override def hasValidProps() = true

    override def initModel(modelProps: Map[String, String], mktStubs: Seq[MarketStub], ctx: MarketDataDistributor) = {
        mdDistributor = ctx
        marketStubs = mktStubs.toArray
        modelProperties = modelProps
        applyProperties(modelProps)
    }

    /**
     * @param intr - interval to generate ohlc
     * @param lengthToMaintain length of enabled histories
     * @return return sequence of TimeSeries objects
     */
    protected def enableOhlcHistory(intr: Interval, lengthToMaintain: Int = -1): IndexedSeq[TimeSeries[Ohlc]] = {
        return (0 until marketStubs.length).map(mdDistributor.activateOhlcTimeSeries(_, intr, lengthToMaintain))
    }

    protected def getTs(mdt: Interval, idx: Int = 0): TimeSeries[Ohlc] = {
        return mdDistributor.activateOhlcTimeSeries(idx, mdt, -1)
    }

    /**
     * method to provide parameters to model
     * @param mprops - configuration params to model
     */
    protected def applyProperties(mprops: Map[String, String])

    protected def onIntervalEnd(dtGmt:Instant) = {}

    protected def position(idx: Int = 0) = marketStubs(idx).position

    override def onBacktestEnd() = {}

    override final def onStep(dtGmt:Instant) = {
        this.dtGmt = dtGmt
        onIntervalEnd(dtGmt)
    }
}
