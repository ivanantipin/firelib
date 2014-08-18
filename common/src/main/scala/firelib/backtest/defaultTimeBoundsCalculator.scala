package firelib.backtest

import java.time.Instant

import firelib.common.ModelConfig
import firelib.utils.DateTimeExt._

object defaultTimeBoundsCalculator extends TimeBoundsCalculator{

    def calcStartDate(cfg: ModelConfig): Instant = {
        var startDtGmt = if (cfg.startDateGmt == null) Instant.EPOCH else cfg.startDateGmt.parseStandard

        startDtGmt = cfg.backtestStepInterval.roundTime(startDtGmt)

        val readerFactory: DefaultReaderFactory = new DefaultReaderFactory(cfg.dataServerRoot)

        val readers = readerFactory.apply(cfg.tickerConfigs, startDtGmt)

        val maxReadersStartDate = readers.maxBy(r =>r.current.DtGmt.getEpochSecond).current.DtGmt

        readers.foreach(_.close())

        return if (maxReadersStartDate.isAfter(startDtGmt)) maxReadersStartDate else startDtGmt

}

    override def apply(cfg : ModelConfig): (Instant, Instant) = {
        val startDt: Instant = calcStartDate(cfg)
        val endDt: Instant = if (cfg.endDate == null) Instant.now() else cfg.endDate.parseStandard
        return (startDt, endDt)
    }
}
