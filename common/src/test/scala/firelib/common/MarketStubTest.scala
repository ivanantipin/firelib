import java.time.Instant

import firelib.common.{TradeGateCallbackAdapter, _}
import firelib.common.marketstub.MarketStubImpl
import org.junit.{Assert, Test}

import scala.collection.mutable.ArrayBuffer


class MarketStubTest {

    @Test
    def TestMarketOrder() {
        var (stub, dtNow, trades) = createStub()

        var bid = 1.5
        var ask = 2.5
        var qty = 2

        stub.updateBidAskAndTime(bid, ask, dtNow)


        stub.submitOrders(List(new Order(OrderType.Market, 0, qty, Side.Buy,"sec","id")))

        Assert.assertEquals(1, trades.length)
        Assert.assertEquals(qty, trades(0).qty)
        Assert.assertEquals(ask, trades(0).price, 0.001)

        Assert.assertEquals(2, stub.position)

        var sellQty = 2
        stub.submitOrders(List(new Order(OrderType.Market, 0, sellQty, Side.Sell,"sec","id1")))

        Assert.assertEquals(2, trades.length)
        Assert.assertEquals(sellQty, trades(1).qty)
        Assert.assertEquals(bid, trades(1).price, 0.001)
        Assert.assertEquals(0, stub.position)

    }

    @Test
    def TestLimitOrderPriceBetterThanMarket() = {
        var (stub, dtNow, trades) = createStub()

        var bid = 1.5
        var ask = 2.5
        stub.updateBidAskAndTime(bid, ask, dtNow)

        var qty = 2
        stub.submitOrders(List(new Order(OrderType.Limit, 3, qty, Side.Buy,"sec","id")))

        Assert.assertEquals(1, trades.length)
        Assert.assertEquals(qty, trades(0).qty)
        Assert.assertEquals(3, trades(0).price, 0.001)

        var sellQty = 2
        stub.submitOrders(List(new Order(OrderType.Limit, 1, sellQty, Side.Sell,"sec","id1")))

        Assert.assertEquals(2, trades.length)
        Assert.assertEquals(sellQty, trades(1).qty)
        Assert.assertEquals(1, trades(1).price, 0.001)
    }

    @Test
    def TestLimitOrder() = {
        var (stub, dtNow, trades) = createStub()

        stub.updateBidAskAndTime(1, 3, dtNow)

        var qty = 2
        var sellQty = 2
        stub.submitOrders(List(new Order(OrderType.Limit, 1.5, qty, Side.Buy,"sec","id")))
        stub.submitOrders(List(new Order(OrderType.Limit, 2.5, sellQty, Side.Sell,"sec","id1")))
        Assert.assertEquals(0, trades.length)

        stub.updateBidAskAndTime(2.6, 3.6, dtNow)
        Assert.assertEquals(1, trades.length)
        Assert.assertEquals(sellQty, trades(0).qty)
        Assert.assertEquals(2.5, trades(0).price, 0.001)

        stub.updateBidAskAndTime(1, 1.5, dtNow)
        Assert.assertEquals(1, trades.length)

        stub.updateBidAskAndTime(1, 1.4, dtNow)

        Assert.assertEquals(2, trades.length)
        Assert.assertEquals(qty, trades(1).qty)
        Assert.assertEquals(1.5, trades(1).price, 0.001)

    }

    @Test
    def TestStopOrder() = {
        var (stub, dtNow, trades) = createStub()

        stub.updateBidAskAndTime(1.5, 2.5, dtNow)

        var qty = 2
        stub.submitOrders(List(new Order(OrderType.Stop, 2.5, qty, Side.Buy,"sec","id")))
        stub.submitOrders(List(new Order(OrderType.Stop, 1.5, qty, Side.Sell,"sec","id1")))

        Assert.assertEquals(0, trades.length)

        stub.updateBidAskAndTime(2.4, 3, dtNow)

        Assert.assertEquals(1, trades.length)
        Assert.assertEquals(3, trades(0).price, 0.001)
        Assert.assertEquals(Side.Buy, trades(0).side)

        stub.updateBidAskAndTime(1.4, 1.5, dtNow)

        Assert.assertEquals(2, trades.length)
        Assert.assertEquals(1.4, trades(1).price, 0.001)
        Assert.assertEquals(Side.Sell, trades(1).side)

    }

    private def createStub(): (MarketStubImpl, Instant, Seq[Trade]) = {
        var stub = new MarketStubImpl("ss")



        val trades = new ArrayBuffer[Trade]()

        stub.addCallback(new TradeGateCallbackAdapter(trades += _))
        return (stub, Instant.now(), trades)
    }

    @Test
    def TestCloseAll() = {
        var (stub, dtNow, trades) = createStub()

        stub.updateBidAskAndTime(1, 3, dtNow)

        var qty = 2
        var sellQty = 1
        stub.submitOrders(List(new Order(OrderType.Limit, 1.5, qty, Side.Buy,"sec","id")))
        stub.submitOrders(List(new Order(OrderType.Market, 2.5, sellQty, Side.Sell,"sec","id1")))
        Assert.assertEquals(1, trades.length)
        Assert.assertEquals(-1, stub.position)

        Assert.assertEquals(1, stub.orders.length)

        stub.flattenAll(None)

        Assert.assertEquals(0, stub.position)

        Assert.assertEquals(0, stub.orders.length)

        Assert.assertEquals(2, trades.length)

        Assert.assertEquals(Side.Buy, trades(1).side)

        Assert.assertEquals(sellQty, trades(1).qty)

    }

}


