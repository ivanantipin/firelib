package firelib.execution

import java.nio.file.{Path, Paths}

import firelib.common._
import firelib.utils.JacksonWrapper
;

object starter {

    val stratsDir: Path = Paths.get("./runtime/src/main/stratsdir")
    
    def main(args : Array[String]) {
        genSampleModelRuntimeConfigForSpyDiaStrat()
        folderStarter.start(stratsDir.toAbsolutePath.toString)
    }

    /**
     * NOTE! that you need to run backtest of sample spydia strategy before running this method
     *  to generate modelconfig json
     */
    def genSampleModelRuntimeConfigForSpyDiaStrat(){
        genSampleRuntimeModelConfig(stratsDir.resolve("spydia.json").toAbsolutePath.toString)
    }



    def genSampleRuntimeModelConfig(fileName : String) = {
        val cfgFile: String = "./strats/src/main/sampleRoot/reportRoot/spydia/cfg.json"
        val spyDiaModelConfig = JacksonWrapper.deserialize(cfgFile,classOf[ModelConfig])

        //need to replace optimization params by concrete ones
        //and switch backtest mode to SimpleRun
        spyDiaModelConfig.optParams.clear()
        spyDiaModelConfig.addCustomParam("trading.hour","10")
        spyDiaModelConfig.backtestMode = BacktestMode.SimpleRun
        //in that dir you can double check performance of strategy after running it
        spyDiaModelConfig.reportRoot = stratsDir.resolve("report").toAbsolutePath.toString

        val file: String = Paths.get(cfgFile).toAbsolutePath.toString

        val cfg = new ModelRuntimeConfig()
        cfg.modelConfig = spyDiaModelConfig
        cfg.gatewayConfig = Map("port"->"4001","client.id"->"1")
        cfg.gatewayType="IB"
        //need to run backtest as we need to initialize quantile
        cfg.runBacktest = true
        JacksonWrapper.serialize(cfg, fileName)
    }
}
