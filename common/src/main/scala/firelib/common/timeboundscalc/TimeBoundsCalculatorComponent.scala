package firelib.common.timeboundscalc

import java.time.Instant

import firelib.common.config.ModelConfig
import firelib.common.misc.dateUtils._
import firelib.common.reader.ReadersFactoryComponent

/**
 * Created by ivan on 9/4/14.
 */
trait TimeBoundsCalculatorComponent {
    this : ReadersFactoryComponent =>

    val timeBoundsCalculator : TimeBoundsCalculator = new TimeBoundsCalculatorImpl()

    class TimeBoundsCalculatorImpl extends TimeBoundsCalculator{

        def calcStartDate(cfg: ModelConfig): Instant = {
            var startDtGmt = if (cfg.startDateGmt == null) Instant.EPOCH else cfg.startDateGmt.parseStandard

            val readers = readersFactory.apply(cfg.tickerConfigs, startDtGmt)

            val maxReadersStartDate = readers.maxBy(r =>r.current.DtGmt.getEpochSecond).current.DtGmt

            readers.foreach(_.close())

            val ret = if (maxReadersStartDate.isAfter(startDtGmt)) maxReadersStartDate else startDtGmt

            return cfg.backtestStepInterval.roundTime(ret)

        }

        override def apply(cfg : ModelConfig): (Instant, Instant) = {
            val startDt: Instant = calcStartDate(cfg)
            val endDt: Instant = if (cfg.endDate == null) Instant.now() else cfg.endDate.parseStandard
            return (startDt, endDt)
        }
    }

}
