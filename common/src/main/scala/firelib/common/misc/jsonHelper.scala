package firelib.common.misc

import java.io.{File, StringWriter}
import java.nio.file.{Files, Paths, StandardOpenOption}

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.collection.JavaConversions._

/**
 * Created by ivan on 9/5/14.
 */
object jsonHelper {

    val mapper = new ObjectMapper()
    mapper.enable(SerializationFeature.INDENT_OUTPUT)
    mapper.registerModule(DefaultScalaModule)

    def toJsonString(obj: Any ): String = {
        val writer = new StringWriter()
        mapper.writeValue(writer, obj)
        writer.toString
    }


    def serialize(value: Any, fileName : String): Unit = {
        val writer = new StringWriter()
        mapper.writeValue(writer, value)
        Files.write(Paths.get(fileName), List(writer.toString), StandardOpenOption.CREATE)
    }

    def deserialize[T](fileName: String, clazz : Class[T]): T =
        mapper.readValue(new File(fileName), clazz)
}
