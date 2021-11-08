package dimastatz.flumenz.tests

import java.sql.Timestamp
import dimastatz.flumenz.tests.utils._
import org.scalatest.funsuite.AnyFunSuite
import dimastatz.flumenz.sessions.SessionPipeline

//noinspection SpellCheckingInspection
class SessionizationTest extends AnyFunSuite with SparkTest {
  private val sleep = 100
  private val iteration = 10
  private val session = getSession()

  test("testSessionization") {
    val pipeline = new SessionPipeline(session, 1)

    val ts = new Timestamp(System.currentTimeMillis())
    val kafka = new KafkaStreamMock("cdnlogs", ts, getSession())
    val df = kafka.createStream()
    assert(df.isStreaming)

    val query = kafka.createQuery(pipeline.aggregate(df, watermark = 1))
    assert(query.isActive)

    Range(0, iteration).foreach(i => {
      val ts = new Timestamp(System.currentTimeMillis())
      val batch = List(s"${i % (iteration / 3 + 1)}, ${ts.toString}")
      kafka.write(batch, false)
      Thread.sleep(sleep)
    })

    query.processAllAvailable()
    assert(kafka.dispose())
    query.stop()
  }
}
