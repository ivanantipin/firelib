package firelib.common.core

import java.time.Instant

import firelib.common.MarketDataType
import firelib.common.agenda.AgendaComponent
import firelib.common.interval.IntervalServiceComponent
import firelib.common.mddistributor.MarketDataDistributorComponent
import firelib.common.reader.{MarketDataReader, ReadersFactoryComponent}
import firelib.common.timeboundscalc.TimeBoundsCalculatorComponent
import firelib.common.timeservice.TimeServiceManagedComponent
import firelib.domain.{Ohlc, Tick, Timed}

trait BacktestComponent {

    this: TimeBoundsCalculatorComponent
      with ModelConfigContext
      with ReadersFactoryComponent
      with MarketDataDistributorComponent
      with BindModelComponent
      with TimeServiceManagedComponent
      with AgendaComponent
      with IntervalServiceComponent =>

    val backtest = new Backtest

    class Backtest {


        def stepFunc() : Unit = {
            intervalService.onStep(timeServiceManaged.currentTime)
            val nextTime = timeServiceManaged.currentTime.plus(modelConfig.stepInterval.duration)
            agenda.addEvent(nextTime, stepFunc)
        }

        var readEnd = false

        val readerFunctions = new Array[()=>Unit](modelConfig.instruments.length)

        def backtest(): Unit = {

            prep()

            while (!readEnd){
                agenda.next()
            }

            models.foreach(_.onBacktestEnd())
        }

        def stepUntil(dtEnd : Instant): Unit = {
            while (timeServiceManaged.dtGmt.isBefore(dtEnd)){
                agenda.next()
            }
        }



        def prep() {
            val bounds = timeBoundsCalculator.apply(modelConfig)
            val readers: Seq[MarketDataReader[Timed]] = modelConfig.instruments.map(readersFactory(_, bounds._1))

            timeServiceManaged.dtGmt = Instant.EPOCH

            for (idx <- 0 until modelConfig.instruments.length) {

                if (modelConfig.instruments(idx).mdType == MarketDataType.Ohlc) {
                    ohlcLambda(readers, idx)
                } else {
                    tickLambda(readers, idx)
                }
            }

            val time: Instant = modelConfig.stepInterval.roundTime(bounds._1)

            agenda.addEvent(time, stepFunc)
        }

        def tickLambda(readers: Seq[MarketDataReader[Timed]], idx: Int) {
            val reader: MarketDataReader[Tick] = readers(idx).asInstanceOf[MarketDataReader[Tick]]
            readerFunctions(idx) = () => {
                marketDataDistributor.onTick(idx, reader.current)

                while (reader.read() && reader.current.dtGmt == timeServiceManaged.currentTime) {
                    marketDataDistributor.onTick(idx, reader.current)
                }

                if (reader.current == null) {
                    readEnd = true
                } else {
                    agenda.addEvent(reader.current.DtGmt, readerFunctions(idx))
                }
            }
            if (reader.current != null) {
                agenda.addEvent(reader.current.DtGmt, readerFunctions(idx))
            } else {
                readEnd = true
            }
        }

        def ohlcLambda(readers: Seq[MarketDataReader[Timed]], idx: Int) {
            val reader: MarketDataReader[Ohlc] = readers(idx).asInstanceOf[MarketDataReader[Ohlc]]
            readerFunctions(idx) = () => {
                marketDataDistributor.onOhlc(idx, reader.current)
                if (!reader.read()) {
                    readEnd = true
                } else {
                    agenda.addEvent(reader.current.DtGmt, readerFunctions(idx))
                }
            }
            if (reader.current != null) {
                agenda.addEvent(reader.current.dtGmtEnd, readerFunctions(idx))
            } else {
                readEnd = true
            }
        }
    }

}
