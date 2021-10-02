package dimastatz.flumenz.tests

import scala.io.Source
import com.typesafe.config.Config
import dimastatz.flumenz.cdnquality.CdnQualityPipeline

import scala.collection.JavaConverters._
import org.apache.spark.sql.SparkSession
import org.scalatest.funsuite.AnyFunSuite

class CdnPerformanceTests extends AnyFunSuite with SparkTest {
  val config: Config = getConfig
  val session: SparkSession = getSession("testCdnQualityFlow")

  test(testName = "testKafkaTopics") {
    val list = config.getStringList("conf.spark.kafka.topics").asScala.toList
    assert(list.contains("test1") && list.contains("test2") && list.length == 2)
  }

  test(testName = "testCdnQualityFlow") {
    import session.sqlContext.implicits._
    val lines = Source.fromResource("cdn_log.json").getLines().toList

    val df = lines
      .map(l => (toTimestamp("2021-01-01T00:20:28"), "cdnlogs", l))
      .toDF("timestamp", "topic", "value")

    df.show()
    assert(df.count() == 9)

    val result = CdnQualityPipeline.query(df)
    result.show()
  }
}
