package firelib.backtest

import java.nio.file.{Path, Paths}
import java.time.Instant
import java.util.function.Supplier

import firelib.common._
import firelib.parser.{CommonIniSettings, IHandler, Parser, TokenGenerator}


class DefaultReaderFactory(val dsRoot : String) extends ReadersFactory {


    val ohlcFactory = new Supplier[Ohlc]{
        override def get(): Ohlc = return new Ohlc()
    }

    val tickFactory = new Supplier[Tick]{
        override def get(): Tick = return new Tick()
    }


    private def createReader[T <: Timed](cfg : TickerConfig, factory : Supplier[T]) : ISimpleReader[T] ={
        val path: Path = Paths.get(dsRoot, cfg.Path)
        val iniFile: String = path.getParent.resolve("common.ini").toAbsolutePath.toString
        val generator: TokenGenerator = new TokenGenerator(new CommonIniSettings().initFromFile(iniFile))
        return new Parser[T](path.toAbsolutePath.toString, generator.handlers.asInstanceOf[Array[IHandler[T]]], factory)
    }

    override def apply(tickerIds: Seq[TickerConfig], startDtGmt: Instant): Seq[ISimpleReader[Timed]] = {
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
