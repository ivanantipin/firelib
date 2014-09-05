package duka;




import com.dukascopy.api.*;

public class DukaStratDel implements IStrategy{
    private IEngine engine;
    private IHistory history;
    private IConsole console;
    private IContext context;
    private Instrument instrument = null;
    private double slPips;
    private IOrder order;
    private double amount = 100;
    private double tpPipsOnProfit;
    private double tpPipsOnLoss;

    @Override
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.history = context.getHistory();
        this.console = context.getConsole();
        this.context = context;
        // subscribe the instrument that we are going to work with
        context.setSubscribedInstruments(java.util.Collections.singleton(instrument));
        // Fetching previous daily bar from history
        IBar prevDailyBar = history.getBar(instrument, Period.DAILY, OfferSide.ASK, 1);
        // Identifying the side of the initial order
        IEngine.OrderCommand orderCmd = prevDailyBar.getClose() > prevDailyBar.getOpen()
                ? IEngine.OrderCommand.BUY
                : IEngine.OrderCommand.SELL;
        // submitting the order with the specified amount, command and take profit
        submitOrder(100, orderCmd, 1.0);
    }

    private void submitOrder(double amount, IEngine.OrderCommand orderCmd, double tpPips) throws JFException {
        double slPrice, tpPrice;
        ITick lastTick = history.getLastTick(instrument);
        // Calculating stop loss and take profit prices
        if (orderCmd == IEngine.OrderCommand.BUY) {
            slPrice = lastTick.getAsk() - slPips * instrument.getPipValue();
            tpPrice = lastTick.getAsk() + tpPips * instrument.getPipValue();
        } else {
            slPrice = lastTick.getBid() + slPips * instrument.getPipValue();
            tpPrice = lastTick.getBid() - tpPips * instrument.getPipValue();
        }
        // Submitting the order for the specified instrument at the current market price
        order = engine.submitOrder(orderCmd.toString() + System.currentTimeMillis(), instrument, orderCmd, amount, 0, 20, slPrice, tpPrice);
    }

    @Override
    public void onTick(Instrument instrument, ITick iTick) throws JFException {

    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar iBar, IBar iBar2) throws JFException {

    }

    @Override
    public void onMessage(IMessage message) throws JFException {
        if (message.getType() != IMessage.Type.ORDER_CLOSE_OK
                || !message.getOrder().equals(order) //only respond to our own order close
                ) {
            return;
        }
        console.getInfo().format("%s closed with P/L %.1f pips", order.getLabel(), order.getProfitLossInPips()).println();
        if (message.getReasons().contains(IMessage.Reason.ORDER_CLOSED_BY_TP)) {
            // on close by TP we keep the order direction
            submitOrder(amount, order.getOrderCommand(), tpPipsOnProfit);
        } else if (message.getReasons().contains(IMessage.Reason.ORDER_CLOSED_BY_SL)) {
            //  on close by SL we change the order direction and use other TP distance
            IEngine.OrderCommand orderCmd = order.isLong() ? IEngine.OrderCommand.SELL : IEngine.OrderCommand.BUY;
            submitOrder(amount, orderCmd, tpPipsOnLoss);
        } else {
            //on manual close or close by another strategy we stop our strategy
            console.getOut().println("Order closed either from outside the strategy. Stopping the strategy.");
            context.stop();
        }
    }

    @Override
    public void onAccount(IAccount iAccount) throws JFException {

    }

    @Override
    public void onStop() throws JFException {

    }
}
