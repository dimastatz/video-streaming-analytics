package dimastatz.flumenz

import org.apache.spark.sql._

trait Pipeline {
  def getName: String
  def getPartitions: String
  def query(df: DataFrame): DataFrame
}
