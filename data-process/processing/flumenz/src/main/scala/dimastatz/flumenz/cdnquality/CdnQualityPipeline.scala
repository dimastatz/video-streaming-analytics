package dimastatz.flumenz.cdnquality

import scala.io.Source
import java.sql.Timestamp
import dimastatz.flumenz._
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._

object CdnQualityPipeline extends Pipeline {
  override def getName: String = "CdnQuality"

  override def getPartitions: List[String] = List("exec_dt")

  override def query(df: DataFrame): DataFrame = {
    val schema = readSchema()
    val convertedTimestampUdf = udf(convertTimestamp _)
    val unpackJsonBatchUdf = udf(unpackJsonBatch _)

    df
      .select("timestamp", "value", "topic")
      .filter(col("topic") === "cdnlogs")
      .select("value", "timestamp")
      .withColumn("exec_dt", convertedTimestampUdf(col("timestamp")))
      .drop("timestamp")
      .withColumn("value", unpackJsonBatchUdf(col("value")))
      .withColumn("value", explode(col("value")))
      .withColumn("value", from_json(col("value"), schema))
  }

  def unpackJsonBatch(json: String): Array[String] = {
    import dimastatz.flumenz.utilities.Extensions._
    json.parse
  }

  def convertTimestamp(ts: Timestamp): String = {
    import dimastatz.flumenz.utilities.Extensions._
    ts.convertTimestamp()
  }

  def readSchema(resource: String = "schemas/cdn_ec.json"): StructType = {
    import dimastatz.flumenz.utilities.Extensions._
    Source.fromResource(resource).readSchema()
  }
}
