package dimastatz.flumenz.cdnquality

import java.sql.Timestamp
import scala.io.Source
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
    val convertTimestampUdf = udf(convertTimestamp _)
    val convertUnixTimeUdf = udf(convertUnixTime _)

    val result = df
      .select("timestamp", "value", "topic")
      .filter(col("topic") === "cdnlogs")
      .withColumn("exec_dt", convertTimestampUdf(col("timestamp"), lit("yyyyMMddHH")))
      .withColumn("value", unpackJsonBatchUdf(col("value")))
      .withColumn("value", explode(col("value")))
      .withColumn("value", from_json(col("value"), schema))
      .filter(col("value.status_code").isNotNull)
      .select(col("value.*"), col("exec_dt"))
      .withColumn("dt", convertUnixTimeUdf(col("timestamp")))
      .withColumn("owner_id", getOwnerUdf(col("rewritten_path")))
      .select("exec_dt", "dt", "owner_id", "pop", "status_code", "write_time")

    result
  }

  def convertUnixTime(x: Double): String = {
    import dimastatz.flumenz.utilities.Extensions._
    val ts = new Timestamp(x.toLong * 1000)
    ts.convertTimestamp("yyyyMMddHHmm")
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
