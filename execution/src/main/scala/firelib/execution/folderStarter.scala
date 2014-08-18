package firelib.execution

import java.io.{File, FileFilter}

import firelib.utils.JacksonWrapper

import scala.collection.mutable.ArrayBuffer


object folderStarter {
    val lst = new ArrayBuffer[ModelRuntimeContainer]()

    val jsonFilter = new FileFilter {
        override def accept(pathname: File): Boolean = pathname.getName.endsWith(".json")
    }

    def start(confDir: String) {
        for (f <- new File(confDir).listFiles(jsonFilter)) {
            val cfg = JacksonWrapper.deserialize(f.toPath.toAbsolutePath.toString, classOf[ModelRuntimeConfig])
            val container: ModelRuntimeContainer = new ModelRuntimeContainer(cfg)
            container.start()
            lst += container
        }
    }

}
