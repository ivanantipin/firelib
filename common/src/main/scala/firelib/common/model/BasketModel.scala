package firelib.common.model

import java.time.Instant

import firelib.common.Trade
import firelib.common.interval.Interval
import firelib.common.marketstub.OrderManager
import firelib.common.mddistributor.MarketDataDistributor
import firelib.common.timeseries.TimeSeries
import firelib.domain.Ohlc

import scala.collection.immutable.IndexedSeq

/**
 * main base class for all strategies
 */
abstract class BasketModel extends Model{

    /**
     * variable holds model properties
     */
    private var modelProperties: Map[String, String] = _

    protected var mdDistributor: MarketDataDistributor = _

    /**
     * this variable holds current time
     */
    protected var dtGmt: Instant  = Instant.MIN


    private var oms: Array[OrderManager] =_

    override def properties: Map[String, String] = modelProperties

    override def name: String = getClass.getName

    override def orderManagers: Seq[OrderManager] = oms

    override def trades: Seq[Trade] = oms.flatMap(_.trades)

    override def hasValidProps() = true

    override def initModel(modelProps: Map[String, String], oms: Seq[OrderManager], distr: MarketDataDistributor) = {
        mdDistributor = distr
        this.oms = oms.toArray
        modelProperties = modelProps
        applyProperties(modelProps)
    }

    /**
     * @param intr - interval to generate ohlc
     * @param lengthToMaintain length of enabled histories
     * @return return sequence of TimeSeries objects
     */
    protected def enableOhlcHistory(intr: Interval, lengthToMaintain: Int = -1): IndexedSeq[TimeSeries[Ohlc]] = {
        return (0 until oms.length).map(mdDistributor.activateOhlcTimeSeries(_, intr, lengthToMaintain))
    }

    /**
     * method to provide parameters to model
     * @param mprops - configuration params to model
     */
    protected def applyProperties(mprops: Map[String, String])

    protected def onIntervalEnd(dtGmt:Instant) = {}

    override def onBacktestEnd() = {}

    override final def onStep(dtGmt:Instant) = {
        this.dtGmt = dtGmt
        onIntervalEnd(dtGmt)
    }
}
