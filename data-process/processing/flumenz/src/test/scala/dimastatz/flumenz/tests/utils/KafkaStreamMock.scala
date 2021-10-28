package dimastatz.flumenz.tests.utils

import org.apache.commons.io.FileUtils
import org.apache.spark.sql._
import org.apache.spark.sql.execution.streaming.MemoryStream
import org.apache.spark.sql.functions.lit

import java.io.File
import java.nio.file._
import java.sql.Timestamp

class KafkaStreamMock(topic: String, ts: Timestamp, session: SparkSession) {
  import session.implicits._
  implicit val ctx: SQLContext = session.sqlContext

  private var kafkaDf: Option[DataFrame] = None
  private var query: Option[streaming.StreamingQuery] = None
  val memoryStream: MemoryStream[String] = MemoryStream[String]
  val tempDir: File = Files.createTempDirectory("kafka-test-").toFile

  def createStream(): DataFrame = {
    kafkaDf = Some(
      memoryStream
        .toDF()
        .withColumn("topic", lit(topic))
        .withColumn("timestamp", lit(ts))
        .withColumn("key", lit("key"))
    )

    kafkaDf.get
  }

  def createQuery[T](df: Dataset[T]): streaming.StreamingQuery = {
    query = Some(
      df.writeStream
        .format("memory")
        .queryName("test")
        .outputMode("Update")
        .option("checkpointLocation", tempDir.getAbsolutePath)
        .start()
    )

    query.get
  }

  def write(batch: List[String], processStream: Boolean = true, showResult: Boolean = true): DataFrame = {
    memoryStream.addData(batch)

    if (processStream) {
      query.get.processAllAvailable()
    }

    if (showResult) {
      kafkaDf.get.sqlContext.table("test").show(false)
    }
    kafkaDf.get.sqlContext.table("test")
  }

  def dispose(): Boolean = {
    FileUtils.deleteDirectory(tempDir)
    !tempDir.exists()
  }
}
