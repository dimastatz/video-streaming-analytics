package dimastatz.flumenz.tests

import java.util.UUID
import scala.io.Source
import org.apache.spark.sql._
import play.api.libs.json.Json
import com.typesafe.config.Config
import org.apache.spark.sql.functions._
import scala.collection.JavaConverters._
import org.scalatest.funsuite.AnyFunSuite
import dimastatz.flumenz.{Pipeline, Pipelines}
import dimastatz.flumenz.cdnquality.CdnQualityPipeline
import dimastatz.flumenz.tests.utils.SparkTest

class CdnPerformanceTests extends AnyFunSuite with SparkTest {
  val config: Config = getConfig
  val session: SparkSession = getSession("testCdnQualityFlow")

  test(testName = "testKafkaTopics") {
    val list = config.getStringList("conf.spark.kafka.topics").asScala.toList
    assert(list.contains("test1") && list.contains("test2") && list.length == 2)
  }

  test(testName = "testCdnQualityFlow") {
    import session.sqlContext.implicits._
    import dimastatz.flumenz.utilities.Extensions._
    val lines = Source.fromResource("cdn_log.json").getLines().toList

    val df = lines
      .map(l => (UUID.randomUUID().toString, toTimestamp("2021-01-01T00:20:28"), "cdnlogs", l))
      .toDF("key", "timestamp", "topic", "value")
      .getKafkaLabels

    assert(df.count() == 9)
    assert(df.columns.length == 4)
    assert(CdnQualityPipeline.getName == "CdnQuality")
    assert(CdnQualityPipeline.getPartitions.contains("exec_dt"))

    val result = CdnQualityPipeline.normalize(df)
    assert(result.count() == 9)
  }

  test(testName = "testBatchCdnQualityFlow") {
    import session.sqlContext.implicits._
    import dimastatz.flumenz.utilities.Extensions._
    val content = Source.fromResource("cdn_log_batch.json").mkString

    val df = List((UUID.randomUUID().toString, toTimestamp("2021-01-01T00:20:28"), "cdnlogs", content))
      .toDF("key", "timestamp", "topic", "value")
      .getKafkaLabels

    assert(df.count() == 1)
    assert(df.columns.length == 4)
    assert(CdnQualityPipeline.getName == "CdnQuality")
    assert(CdnQualityPipeline.getPartitions.contains("exec_dt"))

    val normalized = CdnQualityPipeline.normalize(df)
    assert(normalized.count() == 9)

    val aggregated = CdnQualityPipeline.query(df)
    assert(aggregated.count() == 6)
  }

  test(testName = "testSchemaLoad") {
    val schema = CdnQualityPipeline.readSchema()
    assert(schema.fields.length == 22)
  }

  test(testName = "testPipelineCreate") {
    val res = Pipelines.create(List("cdnlogs"))
    assert(res.length == 1)

    val dummy = new {} with Pipeline {
      override def getName: String = "dummy"

      override def getPartitions: Seq[String] = Seq("dummy")

      override def query(df: DataFrame): DataFrame = ???
    }

    assert(dummy.getName == "dummy" && dummy.getPartitions.contains("dummy"))
  }

  test(testName = "testBatchParse") {
    import dimastatz.flumenz.utilities.Extensions._
    val content = Source.fromResource("cdn_log_batch.json")
    val result = content.mkString.parseJsonBatch
    assert(result.length == 9)
    val jsonArray = result.map(Json.parse)
    assert(jsonArray.length == 9)
  }

  test(testName = "testBrokenJson") {
    import session.sqlContext.implicits._
    import dimastatz.flumenz.utilities.Extensions._
    val content = Source.fromResource("cdn_log_broken.json").mkString

    val df = List((UUID.randomUUID().toString, toTimestamp("2021-01-01T00:20:28"), "cdnlogs", content))
      .toDF("key", "timestamp", "topic", "value")
      .getKafkaLabels

    assert(df.count() == 1)
    assert(df.columns.length == 4)
    assert(CdnQualityPipeline.getName == "CdnQuality")
    assert(CdnQualityPipeline.getPartitions.contains("exec_dt"))

    val result = CdnQualityPipeline.normalize(df)
    assert(result.count() == 9)
    assert(result.filter(col("status_code").isNull).count() == 1)
  }

  test(testName = "testNonJson") {
    import session.sqlContext.implicits._
    import dimastatz.flumenz.utilities.Extensions._
    val content = Source.fromResource("cdn_log_broken_json.json").mkString

    val df = List((UUID.randomUUID().toString, toTimestamp("2021-01-01T00:20:28"), "cdnlogs", content))
      .toDF("key", "timestamp", "topic", "value")
      .getKafkaLabels

    assert(df.count() == 1)
    assert(df.columns.length == 4)
    assert(CdnQualityPipeline.getName == "CdnQuality")
    assert(CdnQualityPipeline.getPartitions.contains("exec_dt"))

    val result = CdnQualityPipeline.query(df)
    assert(result.count() == 0)
  }

  test(testName = "testOwnerExtraction") {
    import dimastatz.flumenz.utilities.Edgecast._
    val path1 = "/80C078/origin-ausw2/slices/0ce/" +
      "e6cf0c55dac249f0a0f72e7c72e6f6cb/0cea2f39d0f04125ad16ba6f420e6920/FP_IFO0100000069.ts"

    assert(path1.getBeamId.get == "0cea2f39d0f04125ad16ba6f420e6920")
    assert(path1.getOwnerId.get == "e6cf0c55dac249f0a0f72e7c72e6f6cb")
    assert("/80C078/origin-default/ichnaea/u.js".getOwnerId.isFailure)
    assert("/80C078/origin-ausw2/slices/static/static.vtt".getOwnerId.isFailure)
  }

  test("testAggregation") {
    import session.sqlContext.implicits._
    import dimastatz.flumenz.utilities.Extensions._

    val content = Source.fromResource("cdn_log_batch.json").mkString

    val df = List((UUID.randomUUID().toString, toTimestamp("2021-01-01T00:20:28"), "cdnlogs", content))
      .toDF("key", "timestamp", "topic", "value")
      .getKafkaLabels

    assert(df.count() == 1)
    assert(df.columns.length == 4)
    assert(CdnQualityPipeline.getName == "CdnQuality")
    assert(CdnQualityPipeline.getPartitions.contains("exec_dt"))

    val result = CdnQualityPipeline.aggregate(CdnQualityPipeline.normalize(df))
    result.show()

    assert(result.select("total").collect().map(_.getLong(0)).sum == 9)
    assert(result.select("http_error").collect().map(_.getLong(0)).sum == 0)
    assert(result.select("long_response_time").collect().map(_.getLong(0)).sum == 1)
  }
}
