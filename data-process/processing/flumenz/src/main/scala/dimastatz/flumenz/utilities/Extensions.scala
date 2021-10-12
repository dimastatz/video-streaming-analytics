package dimastatz.flumenz.utilities

import java.sql.Timestamp
import java.util.TimeZone
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import scala.io.BufferedSource
import java.text.SimpleDateFormat

object Extensions {
  implicit class KafkaDataFrame(df: DataFrame) {
    def getKafkaLabels: DataFrame = {
      df.selectExpr(
        "CAST(key AS STRING)",
        "CAST(topic AS STRING)",
        "CAST(value AS STRING)",
        "CAST(timestamp AS TIMESTAMP)"
      )
    }
  }

  implicit class TimestampExtensions(ts: Timestamp) {
    def convertTimestamp(pattern: String = "yyyyMMddHH"): String = {
      val dateFormat = new SimpleDateFormat(pattern)
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
      dateFormat.format(ts)
    }
  }

  implicit class BufferedSourceExtensions(bs: BufferedSource) {
    def readSchema(): StructType = {
      DataType.fromJson(bs.mkString).asInstanceOf[StructType]
    }
  }

  implicit class JsonBatch(json: String) {
    case class State(
        counter: Int = 0,
        parts: List[String] = List(),
        acc: String = ""
    )

    def parse: Array[String] = {
      val res = json.foldLeft(State())((state, c) =>
        c match {
          case '{' => State(state.counter + 1, state.parts, state.acc + c)
          case '}' =>
            state.counter match {
              case x if x > 1 => State(state.counter - 1, state.parts, state.acc + c)
              case _          => State(0, state.parts ++ List(state.acc + c))
            }
          case _ =>
            state.acc match {
              case x if x.isEmpty => State(state.counter, state.parts ++ List(c.toString))
              case _              => State(state.counter, state.parts, state.acc + c)
            }
        }
      )
      res.parts.filter(i => i.startsWith("{") && i.endsWith("}")).toArray
    }
  }
}
