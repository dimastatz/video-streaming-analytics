package dimastatz.flumenz.utilities

import scala.util.Try
import java.sql.Timestamp
import java.util.TimeZone
import org.apache.spark.sql._
import scala.io.BufferedSource
import java.text.SimpleDateFormat
import org.apache.spark.sql.types._
import org.apache.commons.lang.StringEscapeUtils

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

  implicit class RichDataFrame(df: DataFrame) {
    def saltedJoin(buildDf: DataFrame, joinExpression: Column, joinType: String, salt: Int): DataFrame = {
      import org.apache.spark.sql.functions._
      val tmpDf = buildDf.withColumn("slt_range", array(Range(0, salt).toList.map(lit): _*))
      val tableDf = tmpDf.withColumn("slt_ratio_s", explode(tmpDf("slt_range"))).drop("slt_range")

      val streamDf = df.withColumn("slt_ratio", monotonically_increasing_id % salt)
      val saltedExpr = streamDf("slt_ratio") === tableDf("slt_ratio_s") && joinExpression
      streamDf.join(tableDf, saltedExpr, joinType).drop("slt_ratio_s").drop("slt_ratio")
    }

    def equalsTo(rightDf: DataFrame): Boolean = {
      compareWith(rightDf)._1
    }

    def compareWith(rightDf: DataFrame): (Boolean, DataFrame, DataFrame) = {
      val left = df.except(rightDf)
      val right = rightDf.except(df)
      (left.isEmpty && right.isEmpty, left, right)
    }
  }

}
