import java.time.Instant

import firelib.common.marketstub.{OrderManagerImpl, TradeGateStub}
import firelib.common.model.withTradeUtils._
import firelib.common.{TradeGateCallbackAdapter, _}
import org.junit.{Assert, Test}

import scala.collection.mutable.ArrayBuffer


class OrderManagerTest {

    @Test
    def TestMarketOrder() {
        var (stub, dtNow, trades,gate) = createStub()

        var bid = 1.5
        var ask = 2.5
        var qty = 2

        gate.updateBidAskAndTime(bid, ask, dtNow)


        stub.managePosTo(2)

        Assert.assertEquals(0, trades.length)
        Assert.assertEquals(0, stub.position)

        gate.updateBidAskAndTime(bid,ask,Instant.now())

        Assert.assertEquals(1, trades.length)
        Assert.assertEquals(qty, trades(0).qty)
        Assert.assertEquals(ask, trades(0).price, 0.001)

        Assert.assertEquals(2, stub.position)

        var sellQty = 2

        stub.managePosTo(0)

        Assert.assertEquals(1, trades.length)

        gate.updateBidAskAndTime(bid,ask,Instant.now())

        Assert.assertEquals(2, trades.length)
        Assert.assertEquals(sellQty, trades(1).qty)
        Assert.assertEquals(bid, trades(1).price, 0.001)
        Assert.assertEquals(0, stub.position)

    }

    @Test
    def TestLimitOrderPriceBetterThanMarket() = {
        var (stub, dtNow, trades,gate) = createStub()

        var bid = 1.5
        var ask = 2.5
        gate.updateBidAskAndTime(bid, ask, dtNow)

        var qty = 2

        stub.buyAtLimit(3,qty)

        Assert.assertEquals(0, trades.length)
        Assert.assertEquals(0, stub.position)

        gate.updateBidAskAndTime(bid, ask, dtNow)

        Assert.assertEquals(1, stub.trades.length)
        Assert.assertEquals(qty, stub.trades(0).qty)
        Assert.assertEquals(3, stub.trades(0).price, 0.001)

        var sellQty = 2
        stub.sellAtLimit(1,sellQty)

        Assert.assertEquals(1, trades.length)
        Assert.assertEquals(qty, trades(0).qty)
        Assert.assertEquals(3, trades(0).price, 0.001)

        gate.updateBidAskAndTime(bid, ask, dtNow)


        Assert.assertEquals(2, trades.length)
        Assert.assertEquals(sellQty, trades(1).qty)
        Assert.assertEquals(1, trades(1).price, 0.001)
    }

    @Test
    def TestLimitOrder() = {
        var (stub, dtNow, trades,gate) = createStub()

        var qty = 2
        var sellQty = 2
        stub.submitOrders(new Order(OrderType.Limit, 1.5, qty, Side.Buy,"sec","id"))
        stub.submitOrders(new Order(OrderType.Limit, 2.5, sellQty, Side.Sell,"sec","id1"))

        gate.updateBidAskAndTime(1, 3, dtNow)

        Assert.assertEquals(0, stub.trades.length)

        gate.updateBidAskAndTime(2.6, 3.6, dtNow)
        Assert.assertEquals(1, stub.trades.length)
        Assert.assertEquals(sellQty, stub.trades(0).qty)
        Assert.assertEquals(2.5, stub.trades(0).price, 0.001)

        gate.updateBidAskAndTime(1, 1.5, dtNow)
        Assert.assertEquals(1, stub.trades.length)

        gate.updateBidAskAndTime(1, 1.4, dtNow)

        Assert.assertEquals(2, stub.trades.length)
        Assert.assertEquals(qty, stub.trades(1).qty)
        Assert.assertEquals(1.5, stub.trades(1).price, 0.001)

    }

    @Test
    def TestStopOrder() = {
        var (stub, dtNow, trades,gate) = createStub()



        var qty = 2
        stub.submitOrders(new Order(OrderType.Stop, 2.5, qty, Side.Buy,"sec","id"))
        stub.submitOrders(new Order(OrderType.Stop, 1.5, qty, Side.Sell,"sec","id1"))

        gate.updateBidAskAndTime(1.5, 2.5, dtNow)

        Assert.assertEquals(0, trades.length)

        gate.updateBidAskAndTime(2.4, 3, dtNow)

        Assert.assertEquals(1, trades.length)
        Assert.assertEquals(3, trades(0).price, 0.001)
        Assert.assertEquals(Side.Buy, trades(0).side)

        gate.updateBidAskAndTime(1.4, 1.5, dtNow)

        Assert.assertEquals(2, trades.length)
        Assert.assertEquals(1.4, trades(1).price, 0.001)
        Assert.assertEquals(Side.Sell, trades(1).side)

    }

    private def createStub(): (OrderManagerImpl, Instant, Seq[Trade], TradeGateStub) = {

        val gate = new TradeGateStub
        var stub = new OrderManagerImpl(gate,"ss")
        val trades = new ArrayBuffer[Trade]()
        stub.addCallback(new TradeGateCallbackAdapter(trades += _))
        return (stub, Instant.now(), trades, gate)
    }

    @Test
    def TestCloseAll() = {
        var (stub, dtNow, trades,gate) = createStub()



        var qty = 2
        var sellQty = 1
        stub.submitOrders(new Order(OrderType.Limit, 1.5, qty, Side.Buy,"sec","id"))
        stub.submitOrders(new Order(OrderType.Market, 2.5, sellQty, Side.Sell,"sec","id1"))
        gate.updateBidAskAndTime(1, 3, dtNow)

        Assert.assertEquals(1, trades.length)
        Assert.assertEquals(-1, stub.position)

        Assert.assertEquals(1, stub.liveOrders.toList.length)

        stub.flattenAll()

        Assert.assertEquals(stub.hasPendingState, true)

        gate.updateBidAskAndTime(1, 3, dtNow)

        Assert.assertEquals(stub.hasPendingState, false)

        Assert.assertEquals(0, stub.position)

        Assert.assertEquals(0, stub.liveOrders.toList.length)

        Assert.assertEquals(2, trades.length)

        Assert.assertEquals(Side.Buy, trades(1).side)

        Assert.assertEquals(sellQty, trades(1).qty)

    }

}


