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
    val unpackJsonBatchUdf = udf(unpackJsonBatch _)
    val convertedTimestampUdf = udf(convertTimestamp _)
    val schema = readSchema()

    df
      .select("timestamp", "value", "topic")
      .filter(col("topic") === "cdnlogs")
      .withColumn("exec_dt", convertedTimestampUdf(col("timestamp"), lit("yyyyMMddHH")))
      .withColumn("exec_dt_min", convertedTimestampUdf(col("timestamp"), lit("yyyyMMddHHmm")))
      .withColumn("value", unpackJsonBatchUdf(col("value")))
      .withColumn("value", explode(col("value")))
      .withColumn("value", from_json(col("value"), schema))
      .select(col("value.*"), col("exec_dt"), col("exec_dt_min"))
      .select("exec_dt", "exec_dt_min", "timestamp", "rewritten_path", "status_code", "write_time", "pop")
      .filter(col("status_code").isNotNull)
  }

  def unpackJsonBatch(json: String): Array[String] = {
    import dimastatz.flumenz.utilities.Extensions._
    json.parseJsonBatch
  }

  def convertTimestamp(ts: Timestamp, pattern: String): String = {
    import dimastatz.flumenz.utilities.Extensions._
    ts.convertTimestamp(pattern)
  }

  def readSchema(resource: String = "schemas/cdn_ec.json"): StructType = {
    import dimastatz.flumenz.utilities.Extensions._
    Source.fromResource(resource).readSchema()
  }
}
