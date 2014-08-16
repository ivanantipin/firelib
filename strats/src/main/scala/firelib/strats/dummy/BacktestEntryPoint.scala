package firelib.strats.dummy

import firelib.backtest.backtestStarter
import firelib.common._


object BacktestEntryPoint {

    def main(args : Array[String]){
        val cfg = new ModelConfig()
        cfg.frequencyIntervalId = Interval.Min1.Name
        cfg.className = classOf[DummyStrat].getName
        cfg.reportRoot = "/home/ivan/tmp/report"
        cfg.dataServerRoot = "/home/ivan/tmp/globaldatabase"
        cfg.tickerConfigs += new TickerConfig("SPY","1MIN/STK/SPYV_1.csv",MarketDataType.Ohlc)
        cfg.mode = ResearchMode.SimpleRun
        backtestStarter.Start(cfg)
    }

}
