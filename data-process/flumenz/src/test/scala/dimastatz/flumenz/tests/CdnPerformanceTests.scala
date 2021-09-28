package dimastatz.flumenz.tests

import com.typesafe.config.Config
import scala.collection.JavaConverters._
import org.apache.spark.sql.SparkSession
import org.scalatest.funsuite.AnyFunSuite
import dimastatz.flumenz.cdnquality.CdnQualityPipeline

class CdnPerformanceTests extends AnyFunSuite with SparkTest {
  val config: Config = getConfig
  val session: SparkSession = getSession("testCdnQualityFlow")

  test(testName = "testKafkaTopics") {
    val list = config.getStringList("conf.spark.kafka.topics").asScala.toList
    assert(list.contains("test1") && list.contains("test2") && list.length == 2)
  }

  test(testName = "testCdnQualityFlow") {
    val res = getClass.getResource("/cdn_log.json")
    val df = session.read.json(res.getPath)
    df.show()
    val query = CdnQualityPipeline.query(df)
    query.show(false)
  }
}
