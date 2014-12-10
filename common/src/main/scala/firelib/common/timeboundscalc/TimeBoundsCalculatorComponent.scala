package firelib.common.timeboundscalc

import java.time.Instant

import firelib.common.config.ModelBacktestConfig
import firelib.common.misc.dateUtils._
import firelib.common.reader.ReadersFactoryComponent

/**

 */
trait TimeBoundsCalculatorComponent {
    this : ReadersFactoryComponent =>

    val timeBoundsCalculator : TimeBoundsCalculator = new TimeBoundsCalculatorImpl()

    class TimeBoundsCalculatorImpl extends TimeBoundsCalculator{

        def calcStartDate(cfg: ModelBacktestConfig): Instant = {
            var startDtGmt = if (cfg.startDateGmt == null) Instant.EPOCH else cfg.startDateGmt.parseStandard

            val readers = cfg.instruments.map(c=>readersFactory.apply(c, startDtGmt))

            val maxReadersStartDate = readers.maxBy(r =>r.current.DtGmt.getEpochSecond).current.DtGmt

            readers.foreach(_.close())

            val ret = if (maxReadersStartDate.isAfter(startDtGmt)) maxReadersStartDate else startDtGmt

            return cfg.stepInterval.roundTime(ret)

        }

        override def apply(cfg : ModelBacktestConfig): (Instant, Instant) = {
            val startDt: Instant = calcStartDate(cfg)
            val endDt: Instant = if (cfg.endDate == null) Instant.now() else cfg.endDate.parseStandard
            return (startDt, endDt)
        }
    }
}