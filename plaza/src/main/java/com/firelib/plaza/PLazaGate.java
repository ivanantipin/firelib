package com.firelib.plaza;

import firelib.common.Order;
import firelib.common.TradeGateCallback;
import firelib.common.threading.ThreadExecutor;
import firelib.execution.TradeGate;
import scala.collection.immutable.Map;

public class PLazaGate implements TradeGate {
    @Override
    public void sendOrder(Order order) {

    }

    @Override
    public void cancelOrder(String orderId) {

    }

    @Override
    public void registerCallback(TradeGateCallback tgc) {

    }

    @Override
    public void configure(Map<String, String> config, ThreadExecutor callbackExecutor) {

    }

    @Override
    public void start() {

    }
}