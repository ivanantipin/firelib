package firelib.strats.dummy

import firelib.backtest.BacktestStarter
import firelib.common._


object BacktestEntryPoint {

    def main(args : Array[String]){
        val cfg = new ModelConfig()
        cfg.intervalName = Interval.Min1.Name
        cfg.className = classOf[DummyStrat].getName
        cfg.reportRoot = "/home/ivan/tmp/report"
        cfg.dataServerRoot = "/home/ivan/tmp/globaldatabase"
        cfg.addTickerId(new TickerConfig("SPY","1MIN/STK/SPYV_1.csv",MarketDataType.Ohlc))
        cfg.mode = ResearchMode.SimpleRun
        BacktestStarter.Start(cfg)
    }

}
