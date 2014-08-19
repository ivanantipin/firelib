package firelib.execution

import java.io.{File, FileFilter}

import firelib.utils.JacksonWrapper
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer


object runtimeModelsFolderStarter {
    val lst = new ArrayBuffer[ModelRuntimeContainer]()

    val log = LoggerFactory.getLogger(getClass)

    val jsonFilter = new FileFilter {
        override def accept(pathname: File): Boolean = pathname.getName.endsWith(".json")
    }

    def runFolderWithModelConfigs(confDir: String) {
        log.info("starting all models in folder " + confDir)
        for (f <- new File(confDir).listFiles(jsonFilter)) {
            log.info("starting config file : " + f)

            val cfg = JacksonWrapper.deserialize(f.toPath.toAbsolutePath.toString, classOf[ModelRuntimeConfig])
            val container: ModelRuntimeContainer = new ModelRuntimeContainer(cfg)
            container.start()
            lst += container
        }
    }

}
