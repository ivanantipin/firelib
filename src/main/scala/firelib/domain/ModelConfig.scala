import firelib.domain.TickerConfig
import scala.collection.mutable._

class ModelConfig {
  val TickerIds = ArrayBuffer[TickerConfig]()

  var StartDateGmt: String = _

  var EndDate: String = _

  var DataServerRoot: String = _

  var BinaryStorageRoot: String = _

  var ReportRoot: String = _

  var ClassName: String = _

  var IntervalName: String = _

  val CustomParams = HashMap[String, String]()

  val OptParams: ArrayBuffer[OptimizedParameter] = ArrayBuffer()


  var OptBatchSize = 500

  var OptThreadNumber = 1

  var OptMinNumberOfTrades = 1

  var OptimizedPeriodDays = -1

  var Mode = ResearchMode.SimpleRun;

  def AddTickerId(tickerId: TickerConfig): ModelConfig = {
    if (TickerIds.contains(tickerId)) {
      throw new Exception("ticker id already present " + tickerId);
    }
    TickerIds += tickerId;
    return this;
  }

  /*
        public Interval GetInterval()
        {
          return Interval.ResolveFromName(IntervalName);
        }


        public StrategyMetric OptimizedMetric = StrategyMetric.SharpeStat;

          public List<StrategyMetric> CalculatedMetrics = new List<StrategyMetric>
                                          {
                                              StrategyMetric.PfStat,
                                              StrategyMetric.PnlStat,
                                              StrategyMetric.SharpeStat,
                                              StrategyMetric.AvgPnlStat
                                          };
  */


  def AddCustomParam(param: String, value: String): ModelConfig = {
    CustomParams(param) = value;
    return this;
  }


}
