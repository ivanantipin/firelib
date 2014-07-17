package firelib.backtest

import firelib.domain.MarketDataType
import org.joda.time.DateTime

class BidAskUpdater(val stub: Array[IMarketStub], val tickerTypes: Array[MarketDataType]) extends IQuoteListener with IStepListener {

    private val bid, ask = new Array[Double](stub.length);


    def AddQuote(int

    idx, RecordQuote quote)
    {
        if (tickerTypes[idx] == TickerType.Tick) {
            bid[idx] = quote.BidPrice;
            ask[idx] = quote.AskPrice;
        }
        else {
            bid[idx] = quote.Close - 0.005;
            ask[idx] = quote.Close + 0.005;
        }
    }

    def OnStep(dtGmt: DateTime) = {
        for (i <- 0 until stub.length) {
            stub(i).UpdateBidAskAndTime(bid(i), ask(i), dtGmt);
        }
    }
}
