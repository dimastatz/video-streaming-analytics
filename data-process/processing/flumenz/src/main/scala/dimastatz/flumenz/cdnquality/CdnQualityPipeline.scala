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
    val getOwnerUdf = udf(getOwner _)
    val unpackJsonBatchUdf = udf(unpackJsonBatch _)
    val convertedTimestampUdf = udf(convertTimestamp _)

    val result = df
      .select("timestamp", "value", "topic")
      .filter(col("topic") === "cdnlogs")
      .withColumn("exec_dt", convertedTimestampUdf(col("timestamp"), lit("yyyyMMddHH")))
      .withColumn("value", unpackJsonBatchUdf(col("value")))
      .withColumn("value", explode(col("value")))
      .withColumn("value", from_json(col("value"), schema))
      .select(col("value.*"), col("exec_dt"))
      .select("exec_dt", "timestamp", "rewritten_path", "status_code", "write_time", "pop")
      .filter(col("status_code").isNotNull && col("rewritten_path").isNotNull)
      .withColumn("owner_id", getOwnerUdf(col("rewritten_path")))

    result
  }

  def getOwner(rewrittenPath: String): String = {
    import dimastatz.flumenz.utilities.Edgecast._
    rewrittenPath.getOwnerId.getOrElse("")
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
