package firelib.common.reader

import java.nio.file.{Path, Paths}
import java.time.Instant
import java.util.function.Supplier

import firelib.common.MarketDataType
import firelib.common.config.InstrumentConfig
import firelib.domain.{Ohlc, Tick, Timed}
import firelib.parser.{CommonIniSettings, IHandler, Parser, ParserHandlersProducer}

/**

 */
trait ReadersFactoryComponent {

    val dsRoot : String

    val readersFactory : ReadersFactory = new ReaderFactoryImpl()

    class ReaderFactoryImpl() extends ReadersFactory {


        private val ohlcFactory = new Supplier[Ohlc]{
            override def get(): Ohlc = return new Ohlc()
        }

        private val tickFactory = new Supplier[Tick]{
            override def get(): Tick = return new Tick()
        }


        private def createReader[T <: Timed](cfg : InstrumentConfig, factory : Supplier[T]) : SimpleReader[T] ={
            val path: Path = Paths.get(dsRoot, cfg.path)
            val iniFile: String = path.getParent.resolve("common.ini").toAbsolutePath.toString
            val generator: ParserHandlersProducer = new ParserHandlersProducer(new CommonIniSettings().loadFromFile(iniFile))
            return new Parser[T](path.toAbsolutePath.toString, generator.handlers.asInstanceOf[Array[IHandler[T]]], factory)
        }

        override def apply(tickerIds: Seq[InstrumentConfig], startDtGmt: Instant): Seq[SimpleReader[Timed]] = {
            return tickerIds.map(t=>{
                val parser =
                    if (t.mdType == MarketDataType.Tick)
                        createReader[Tick](t, tickFactory)
                    else
                        createReader[Ohlc](t, ohlcFactory)
                assert(parser.seek(startDtGmt), "failed to find start date " + startDtGmt)
                parser
            })

        }
    }

}
