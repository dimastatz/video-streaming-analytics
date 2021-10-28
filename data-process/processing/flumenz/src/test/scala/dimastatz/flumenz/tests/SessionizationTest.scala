package dimastatz.flumenz.tests

import java.sql.Timestamp
import org.apache.spark.sql._
import dimastatz.flumenz.tests.utils._
import org.apache.spark.sql.functions._
import org.scalatest.funsuite.AnyFunSuite

//noinspection SpellCheckingInspection
class SessionizationTest extends AnyFunSuite with SparkTest {
  private val sleep = 200
  private val iteration = 10
  private val session = getSession()

  test("testSessionization") {

    val ts = new Timestamp(System.currentTimeMillis())
    val kafka = new KafkaStreamMock("events", ts, getSession())
    val df = kafka.createStream()
    assert(df.isStreaming)

    val query = kafka.createQuery(transform(df))
    assert(query.isActive)

    Range(0, iteration).foreach(i => {
      val ts = new Timestamp(System.currentTimeMillis())
      val batch = List(s"${i % (iteration / 2 + 1)}, ${ts.toString}")
      kafka.write(batch, false)
      Thread.sleep(sleep)
    })
    query.processAllAvailable()
    assert(kafka.dispose())
  }

  private def transform(df: DataFrame): DataFrame = {
    import session.implicits._

    val getSession = udf((x: String) => x.split(",").head)
    val getTimestamp = udf((x: String) => Timestamp.valueOf(x.split(",").last))

    df
      .select("value")
      .withColumn("sessionId", getSession(col("value")))
      .withColumn("timestamp", getTimestamp(col("value")))
      .withWatermark("timestamp", "1 minutes")
      .groupBy(window($"timestamp", "1 minutes"), col("sessionId"))
      .count()
      .as("events")
  }
}
