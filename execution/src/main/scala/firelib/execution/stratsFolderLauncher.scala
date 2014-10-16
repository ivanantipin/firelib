package firelib.execution

import java.io.{File, FileFilter}

import firelib.common.misc.jsonHelper
import firelib.execution.config.ModelExecutionConfig
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer


object stratsFolderLauncher {
    val lst = new ArrayBuffer[ModelExecutionLauncher]()

    val log = LoggerFactory.getLogger(getClass)

    val jsonFilter = new FileFilter {
        override def accept(pathname: File): Boolean = pathname.getName.endsWith(".json")
    }

    def runFolderWithModelConfigs(confDir: String) {
        log.info("launching all models in folder " + confDir)
        for (f <- new File(confDir).listFiles(jsonFilter)) {
            log.info("launching model for config file : " + f)
            val cfg = jsonHelper.deserialize(f.toPath, classOf[ModelExecutionConfig])
            val launcher: ModelExecutionLauncher = new ModelExecutionLauncher(cfg)
            launcher.start()
            lst += launcher
        }
    }

    def main(args : Array[String]) {
        stratsFolderLauncher.runFolderWithModelConfigs(args(0))
    }


}
