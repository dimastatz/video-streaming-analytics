package dimastatz.flumenz.utilities

import org.apache.spark.sql.DataFrame

import java.sql.Timestamp
import java.util.TimeZone
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
}
