package dimastatz.flumenz.cdnquality

import java.sql.Timestamp
import dimastatz.flumenz.Pipeline
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

object CdnQualityPipeline extends Pipeline {
  override def getName: String = "CdnQuality"

  override def getPartitions: List[String] = List("exec_dt")

  override def query(df: DataFrame): DataFrame = {
    val convertedTimestampUdf = udf(convertTimestamp _)
    df
      .select("timestamp", "value", "topic")
      .filter(col("topic") === "cdnlogs")
      .select("value", "timestamp")
      .withColumn("exec_dt", convertedTimestampUdf(col("timestamp")))
      .drop("timestamp")
  }

  def convertTimestamp(ts: Timestamp): String = {
    import dimastatz.flumenz.utilities.Extensions._
    ts.convertTimestamp()
  }
}
