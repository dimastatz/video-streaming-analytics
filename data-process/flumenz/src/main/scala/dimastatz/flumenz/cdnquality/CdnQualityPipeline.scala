package dimastatz.flumenz.cdnquality

import dimastatz.flumenz.Pipeline
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col

object CdnQualityPipeline extends Pipeline {
  override def getName: String = "CdnQuality"

  override def getPartitions: List[String] = List("exec_dt")

  override def query(df: DataFrame): DataFrame = {
    df
      .select("timestamp", "value", "topic")
      .filter(col("topic") === "cdn")
      .select("rewritten_path", "status_code")
  }
}
