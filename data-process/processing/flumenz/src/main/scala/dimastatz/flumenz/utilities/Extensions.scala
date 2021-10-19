package dimastatz.flumenz.utilities

import java.sql.Timestamp
import java.util.TimeZone
import org.apache.spark.sql._

import scala.io.BufferedSource
import java.text.SimpleDateFormat
import org.apache.spark.sql.types._
import org.apache.commons.lang.StringEscapeUtils

import scala.util.Try

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

    def parseJsonBatch: Array[String] = {
      Try {
        val stack = scala.collection.mutable.Stack[Int]()
        val list = scala.collection.mutable.MutableList[String]()
        Range(0, json.length).foreach(i =>
          json(i) match {
            case '{' => stack.push(i)
            case '}' =>
              (stack.pop(), stack.length) match {
                case (x, 0) => list += json.substring(x, i + 1)
                case _      => stack.pop()
              }
            case _ =>
          }
        )
        list.map(StringEscapeUtils.unescapeJava).toArray
      }.getOrElse(Array(json))
    }
  }
}
