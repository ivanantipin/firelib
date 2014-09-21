package firelib.common.misc

import java.nio.file.{Path, Paths}

import firelib.common.config.InstrumentConfig
import firelib.common.core.ModelConfigContext
import firelib.domain.Tick
import firelib.parser.{CommonIniSettings, ParserHandlersProducer}


trait TickToPriceConverterComponent {

    this : ModelConfigContext =>

    val tickToPriceConverterFactory : InstrumentConfig=>(Tick=>Double) = new TickToPriceConverterFactory

    class TickToPriceConverterFactory extends (InstrumentConfig=>(Tick=>Double)){

        override def apply(conf : InstrumentConfig): (Tick=>Double) ={
            val path: Path = Paths.get(modelConfig.dataServerRoot, conf.path)
            val iniFile: String = path.getParent.resolve("common.ini").toAbsolutePath.toString
            val parseSettings: CommonIniSettings = new CommonIniSettings().loadFromFile(iniFile)
            val generator: ParserHandlersProducer = new ParserHandlersProducer(parseSettings)
            if(parseSettings.COLUMNFORMAT.contains("P")){
                ((t : Tick)=>t.last)
            }else{
                ((t : Tick)=>(t.bid + t.ask)/2)
            }
        }
    }
}



