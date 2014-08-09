package firelib.execution

import java.io.File

import firelib.robot.{ModelRuntimeConfig, ModelRuntimeContainer}
import firelib.utils.JacksonWrapper

import scala.collection.mutable.ArrayBuffer


object Starter {

    val lst = new ArrayBuffer[ModelRuntimeContainer]()

    def start(confDir : String) {

        for(f <- new File(confDir).listFiles()){
            val cfg = JacksonWrapper.deserialize(f.toPath.toAbsolutePath.toString, classOf[ModelRuntimeConfig])
            val container: ModelRuntimeContainer = new ModelRuntimeContainer(cfg)
            container.Start()
            lst += container
        }
    }

    def main(args : Array[String]) {
        start(args(0))
    }
}
