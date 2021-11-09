package dimastatz.flumenz.tests.utils

import java.io.File
import java.nio.file._
import java.sql.Timestamp
import org.apache.spark.sql._
import org.apache.commons.io.FileUtils
import org.apache.spark.sql.functions._
import org.apache.spark.sql.execution.streaming._

class KafkaStreamMock(topic: String, ts: Timestamp, session: SparkSession) {
  import session.implicits._
  implicit val ctx: SQLContext = session.sqlContext

  private var kafkaDf: Option[DataFrame] = None
  private var query: Option[streaming.StreamingQuery] = None
  val memoryStream: MemoryStream[String] = MemoryStream[String]
  val tempDir: File = Files.createTempDirectory("kafka-test-").toFile

  def createStream(): DataFrame = {
    val df = memoryStream
      .toDF()
      .withColumn("topic", lit(topic))
      .withColumn("timestamp", lit(ts))

    kafkaDf = Some(df)
    df
  }

  def createQuery[T](df: Dataset[T]): streaming.StreamingQuery = {
    query = Some(
      df.writeStream
        .format("console")
        .queryName("test")
        .outputMode("update")
        .option("checkpointLocation", tempDir.getAbsolutePath)
        .start()
    )

    query.get
  }

  def write(batch: List[String], processStream: Boolean = true): Unit = {
    memoryStream.addData(batch)
    if (processStream) {
      query.get.processAllAvailable()
    }
  }

  def dispose(): Boolean = {
    FileUtils.deleteDirectory(tempDir)
    !tempDir.exists()
  }
}
