import java.time.Instant

import firelib.backtest.TradeGateCallbackAdapter
import firelib.common._
import org.junit.{Assert, Test}

import scala.collection.mutable.ArrayBuffer


class MarketStubTest {

    @Test
    def TestMarketOrder() {
        var (stub, dtNow, trades) = createStub();

        var bid = 1.5;
        var ask = 2.5;
        stub.updateBidAskAndTime(bid, ask, dtNow);

        var qty = 2;
        stub.submitOrders(List(new Order(OrderType.Market, 0, qty, Side.Buy)));

        Assert.assertEquals(1, trades.length);
        Assert.assertEquals(qty, trades(0).Qty);
        Assert.assertEquals(ask, trades(0).Price, 0.001);

        Assert.assertEquals(2, stub.Position);

        var sellQty = 2;
        stub.submitOrders(List(new Order(OrderType.Market, 0, sellQty, Side.Sell)));

        Assert.assertEquals(2, trades.length);
        Assert.assertEquals(sellQty, trades(1).Qty);
        Assert.assertEquals(bid, trades(1).Price, 0.001);
        Assert.assertEquals(0, stub.Position);

    }

    @Test
    def TestLimitOrderPriceBetterThanMarket() = {
        var (stub, dtNow, trades) = createStub();

        var bid = 1.5;
        var ask = 2.5;
        stub.updateBidAskAndTime(bid, ask, dtNow);

        var qty = 2;
        stub.submitOrders(List(new Order(OrderType.Limit, 3, qty, Side.Buy)));

        Assert.assertEquals(1, trades.length);
        Assert.assertEquals(qty, trades(0).Qty);
        Assert.assertEquals(3, trades(0).Price, 0.001);

        var sellQty = 2;
        stub.submitOrders(List(new Order(OrderType.Limit, 1, sellQty, Side.Sell)));

        Assert.assertEquals(2, trades.length);
        Assert.assertEquals(sellQty, trades(1).Qty);
        Assert.assertEquals(1, trades(1).Price, 0.001);
    }

    @Test
    def TestLimitOrder() = {
        var (stub, dtNow, trades) = createStub();

        stub.updateBidAskAndTime(1, 3, dtNow);

        var qty = 2;
        var sellQty = 2;
        stub.submitOrders(List(new Order(OrderType.Limit, 1.5, qty, Side.Buy)));
        stub.submitOrders(List(new Order(OrderType.Limit, 2.5, sellQty, Side.Sell)));
        Assert.assertEquals(0, trades.length);

        stub.updateBidAskAndTime(2.6, 3.6, dtNow);
        Assert.assertEquals(1, trades.length);
        Assert.assertEquals(sellQty, trades(0).Qty);
        Assert.assertEquals(2.5, trades(0).Price, 0.001);

        stub.updateBidAskAndTime(1, 1.5, dtNow);
        Assert.assertEquals(1, trades.length);

        stub.updateBidAskAndTime(1, 1.4, dtNow);

        Assert.assertEquals(2, trades.length);
        Assert.assertEquals(qty, trades(1).Qty);
        Assert.assertEquals(1.5, trades(1).Price, 0.001);

    }

    @Test
    def TestStopOrder() = {
        var (stub, dtNow, trades) = createStub();

        stub.updateBidAskAndTime(1.5, 2.5, dtNow);

        var qty = 2;
        stub.submitOrders(List(new Order(OrderType.Stop, 2.5, qty, Side.Buy)));
        stub.submitOrders(List(new Order(OrderType.Stop, 1.5, qty, Side.Sell)));

        Assert.assertEquals(0, trades.length);

        stub.updateBidAskAndTime(2.4, 3, dtNow);

        Assert.assertEquals(1, trades.length);
        Assert.assertEquals(3, trades(0).Price, 0.001);
        Assert.assertEquals(Side.Buy, trades(0).TradeSide);

        stub.updateBidAskAndTime(1.4, 1.5, dtNow);

        Assert.assertEquals(2, trades.length);
        Assert.assertEquals(1.4, trades(1).Price, 0.001);
        Assert.assertEquals(Side.Sell, trades(1).TradeSide);

    }

    private def createStub(): (MarketStub, Instant, Seq[Trade]) = {
        var stub = new MarketStub("ss");



        val trades = new ArrayBuffer[Trade]()

        stub.addCallback(new TradeGateCallbackAdapter(trades += _));
        return (stub, Instant.now(), trades);
    }

    @Test
    def TestCloseAll() = {
        var (stub, dtNow, trades) = createStub();

        stub.updateBidAskAndTime(1, 3, dtNow);

        var qty = 2;
        var sellQty = 1;
        stub.submitOrders(List(new Order(OrderType.Limit, 1.5, qty, Side.Buy)));
        stub.submitOrders(List(new Order(OrderType.Market, 2.5, sellQty, Side.Sell)));
        Assert.assertEquals(1, trades.length);
        Assert.assertEquals(-1, stub.Position);

        Assert.assertEquals(1, stub.orders.length);

        stub.flattenAll();

        Assert.assertEquals(0, stub.Position);

        Assert.assertEquals(0, stub.orders.length);

        Assert.assertEquals(2, trades.length);

        Assert.assertEquals(Side.Buy, trades(1).TradeSide);

        Assert.assertEquals(sellQty, trades(1).Qty);

    }

}


