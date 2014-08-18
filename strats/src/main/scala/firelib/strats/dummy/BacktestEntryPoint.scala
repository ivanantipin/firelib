package firelib.strats.dummy

import firelib.backtest.backtestStarter
import firelib.common._


object BacktestEntryPoint {

    def main(args : Array[String]){
        val cfg = new ModelConfig()
        cfg.backtestStepInterval = Interval.Min1
        cfg.modelClassName = classOf[DummyStrat].getName
        cfg.reportRoot = "/home/ivan/tmp/report"
        cfg.dataServerRoot = "/home/ivan/tmp/globaldatabase"
        cfg.tickerConfigs += new TickerConfig("SPY","1MIN/STK/SPYV_1.csv",MarketDataType.Ohlc)
        cfg.backtestMode = BacktestMode.SimpleRun
        backtestStarter.runBacktest(cfg)
    }

}
