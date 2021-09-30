package dimastatz.flumenz

import org.apache.spark.sql._

trait Pipeline {
  def getName: String
  def getPartitions: Seq[String]
  def query(df: DataFrame): DataFrame
}

object Pipelines {
  def create(): Seq[Pipeline] = ???
}
