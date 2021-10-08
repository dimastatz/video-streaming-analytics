package dimastatz.flumenz

import dimastatz.flumenz.cdnquality.CdnQualityPipeline
import org.apache.spark.sql._

trait Pipeline {
  def getName: String
  def getPartitions: Seq[String]
  def query(df: DataFrame): DataFrame
}

object Pipelines {
  def create(topic: Seq[String]): Seq[Pipeline] = {
    topic map {
      case "cdnlogs" => CdnQualityPipeline
    }
  }
}
