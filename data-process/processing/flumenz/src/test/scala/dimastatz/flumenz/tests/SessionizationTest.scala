package dimastatz.flumenz.tests

import scala.util.Try
import java.sql.Timestamp
import play.api.libs.json.Json
import dimastatz.flumenz.sessions._
import dimastatz.flumenz.tests.utils._
import org.apache.spark.sql.functions._
import org.scalatest.funsuite.AnyFunSuite
import org.apache.spark.sql.types.StructType

//noinspection SpellCheckingInspection
class SessionizationTest extends AnyFunSuite with SparkTest {
  private val sleep = 100
  private val iteration = 1
  private val session = getSession()

  test("testSessionization") {
    val pipeline = new SessionPipeline(session, 1)
    assert(pipeline.getName == "SessionsPipeline")
    assert(pipeline.getPartitions.head == "close_dt")

    val ts = new Timestamp(System.currentTimeMillis() / 1000)
    val kafka = new KafkaStreamMock("cdnlogs", ts, getSession())
    val df = kafka.createStream()

    assert(df.isStreaming)
    assert(Try(pipeline.query(df).count()).isFailure)

    import org.apache.spark.sql.catalyst.ScalaReflection
    val schema = ScalaReflection.schemaFor[Event].dataType.asInstanceOf[StructType]

    val cleanDf = df
      .select("value")
      .withColumn("value", from_json(col("value"), schema))
      .select(col("value.*"))

    cleanDf.printSchema()

    val query = kafka.createQuery(pipeline.aggregate(cleanDf, 1))
    assert(query.isActive)

    Range(0, iteration).foreach(i => {
      val e = Event("1", "sessionOpen", ts)
      val json = Json.writes[Event].writes(e).toString()
      println(json)

      kafka.write(List(json), false)
      Thread.sleep(sleep)
    })

    query.processAllAvailable()
    assert(kafka.dispose())
    query.stop()
  }
}
