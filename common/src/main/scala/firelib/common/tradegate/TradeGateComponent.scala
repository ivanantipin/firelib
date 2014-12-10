package firelib.common.tradegate

import firelib.common.agenda.AgendaComponent
import firelib.common.config.ModelBacktestConfig
import firelib.common.core.{ModelConfigContext, OnContextInited}
import firelib.common.marketstub.{BookStub, LimitOBook, MarketOrderStub, OhlcBidAskUpdater, StopOBook, TickBidAskUpdater, TradeGate, TradeGateDelay}
import firelib.common.mddistributor.{MarketDataDistributor, MarketDataDistributorComponent}
import firelib.common.misc.Topic
import firelib.common.timeservice.{TimeService, TimeServiceComponent}
import firelib.common.{Order, OrderType, Trade}
import firelib.domain.OrderState

import scala.collection.mutable

trait TradeGateComponent {
    var tradeGate : TradeGate = null
}

trait TradeGateStubComponent {
    this : AgendaComponent with TimeServiceComponent with ModelConfigContext with MarketDataDistributorComponent with OnContextInited=>

    var stub: TradeGateStub = null

    var tradeGateDelay : TradeGate = null

    initMethods += (()=>{
        stub = new TradeGateStub(marketDataDistributor, modelConfig, timeService)
        tradeGateDelay = new TradeGateDelay(timeService,modelConfig.networkSimulatedDelayMs,stub,agenda)
    })

}


class TradeGateStub(val marketDataDistributor : MarketDataDistributor, val modelConfig : ModelBacktestConfig, val timeService : TimeService) extends TradeGate{

    val secToBookLimit = new mutable.HashMap[String,BookStub]()

    val secToBookStop = new mutable.HashMap[String,BookStub]()

    val secToMarketOrderStub = new mutable.HashMap[String,MarketOrderStub]()


    for(i <- 0 until modelConfig.instruments.length){
        val ticker =  modelConfig.instruments(i).ticker
        secToBookLimit(ticker) = new BookStub(timeService) with LimitOBook
        marketDataDistributor.listenTicks(i,new TickBidAskUpdater((b,a)=>secToBookLimit(ticker).updateBidAsk(b,a)))
        marketDataDistributor.listenOhlc(i,new OhlcBidAskUpdater(secToBookLimit(ticker)))


        secToBookStop(ticker) = new BookStub(timeService) with StopOBook
        marketDataDistributor.listenTicks(i,new TickBidAskUpdater((b,a)=>secToBookStop(ticker).updateBidAsk(b,a)))
        marketDataDistributor.listenOhlc(i,new OhlcBidAskUpdater(secToBookStop(ticker)))

        secToMarketOrderStub(ticker) = new MarketOrderStub(timeService)
        marketDataDistributor.listenTicks(i,new TickBidAskUpdater((b,a)=>secToMarketOrderStub(ticker).updateBidAsk(b,a)))
        marketDataDistributor.listenOhlc(i,new OhlcBidAskUpdater(secToMarketOrderStub(ticker)))

    }

    /**
         * just order send
         */
    override def sendOrder(order: Order): (Topic[Trade], Topic[OrderState]) = {
        order.orderType match {
            case OrderType.Limit => secToBookLimit(order.security).sendOrder(order)
            case OrderType.Stop => secToBookStop(order.security).sendOrder(order)
            case OrderType.Market => secToMarketOrderStub(order.security).sendOrder(order)
        }

    }

    /**
     * just order cancel
     */
    override def cancelOrder(order: Order): Unit = {
        order.orderType match {
            case OrderType.Limit => secToBookLimit(order.security).cancelOrder(order)
            case OrderType.Stop => secToBookStop(order.security).cancelOrder(order)
            case OrderType.Market => ???
        }

    }
}