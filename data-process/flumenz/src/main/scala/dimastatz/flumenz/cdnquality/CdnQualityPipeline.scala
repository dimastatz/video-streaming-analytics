package dimastatz.flumenz.cdnquality

import dimastatz.flumenz.Pipeline
import org.apache.spark.sql.DataFrame

object CdnQualityPipeline extends Pipeline {
  override def getName: String = "CdnQuality"

  override def getPartitions: List[String] = List("exec_dt")

  override def query(df: DataFrame): DataFrame = {
    df.select("rewritten_path", "status_code")
  }
}
