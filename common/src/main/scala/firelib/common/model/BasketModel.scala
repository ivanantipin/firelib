package firelib.common.model

import java.time.Instant

import firelib.common.core.BindModelComponent
import firelib.common.interval.Interval
import firelib.common.mddistributor.MarketDataDistributorComponent
import firelib.common.ordermanager.OrderManager
import firelib.common.timeseries.TimeSeries
import firelib.common.timeservice.TimeServiceComponent
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

    protected def currentTime: Instant  = bindComp.timeService.currentTime

    var orderManagersFld: Array[OrderManager] =_

    var bindComp : BindModelComponent with TimeServiceComponent with MarketDataDistributorComponent = null

    override def properties: Map[String, String] = modelProperties

    override def name: String = getClass.getName

    override def orderManagers: Seq[OrderManager] = orderManagersFld

    override def initModel(modelProps: Map[String, String]) : Boolean = {
        modelProperties = modelProps
        applyProperties(modelProps)
    }

    /**
     * @param intr - interval to generate ohlc
     * @param lengthToMaintain length of enabled histories
     * @return return sequence of TimeSeries objects
     */
    protected def enableOhlcHistory(intr: Interval, lengthToMaintain: Int = -1): IndexedSeq[TimeSeries[Ohlc]] = {
        return (0 until orderManagers.length).map(bindComp.marketDataDistributor.activateOhlcTimeSeries(_, intr, lengthToMaintain))
    }

    /**
     * method to provide parameters to model
     * @param mprops - configuration params to model
     * returns false if parameters are invalid
     */
    protected def applyProperties(mprops: Map[String, String]) : Boolean



    override def onBacktestEnd() = {
        orderManagers.foreach(_.flattenAll())
    }

}
