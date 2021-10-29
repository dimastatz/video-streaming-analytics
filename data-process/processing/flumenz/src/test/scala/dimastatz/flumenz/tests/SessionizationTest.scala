package dimastatz.flumenz.tests

import java.sql.Timestamp
import org.apache.spark.sql._
import dimastatz.flumenz.tests.utils._
import org.apache.spark.sql.functions._
import org.scalatest.funsuite.AnyFunSuite
import org.apache.spark.sql.streaming.{GroupState, GroupStateTimeout}
import dimastatz.flumenz.tests.SessionizationTest._

//noinspection SpellCheckingInspection
class SessionizationTest extends AnyFunSuite with SparkTest {
  private val sleep = 200
  private val iteration = 0
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
      val batch = List(s"${i % (iteration / 3 + 1)}, ${ts.toString}")
      kafka.write(batch, false)
      Thread.sleep(sleep)
    })
    query.processAllAvailable()
    assert(kafka.dispose())
    query.stop()
  }

  private def transform(df: DataFrame): Dataset[Result] = {
    import session.implicits._

    val getSession = udf((x: String) => x.split(",").head)
    val getTimestamp = udf((x: String) => Timestamp.valueOf(x.split(",").last))

    df
      .select("value")
      .withColumn("sessionId", getSession(col("value")))
      .withColumn("timestamp", getTimestamp(col("value")))
      .withWatermark("timestamp", "1 minutes")
      .groupByKey(i => i.getAs[String]("sessionId"))
      .mapGroupsWithState(timeoutConf = GroupStateTimeout.EventTimeTimeout())(process)
  }

  def transform2(df: DataFrame): DataFrame = {
    val getUser = udf(() => {
      val r = scala.util.Random
      r.nextInt(2) match {
        case 0 => "userA"
        case _ => "userB"
      }
    })
    val getSession = udf((x: String) => x.split(",").head)
    val getTimestamp = udf((x: String) => Timestamp.valueOf(x.split(",").last))

    df
      .select("value")
      .withColumn("user", getUser())
      .withColumn("sessionId", getSession(col("value")))
      .withColumn("timestamp", getTimestamp(col("value")))
      .withWatermark("timestamp", "1 minutes")
      .groupBy(window(col("timestamp"), "1 minutes"), col("sessionId"), col("user"))
      .count()
      .as("events")
      .where(col("user") === "userA")
  }
}

object SessionizationTest {
  case class Result(sessionId: String, closed: Boolean, count: Int)

  def process(sessionId: String, events: Iterator[Row], state: GroupState[Result]): Result = {
    val default = Result(sessionId, false, 0)
    if (state.hasTimedOut) {
      println(s"TO $sessionId")
      val result = state.getOption.getOrElse(default)
      state.remove()
      result
    } else {
      val current = state.getOption.getOrElse(default)
      if (current.count + events.length > 3) {
        state.update(Result(sessionId, true, current.count + events.length))
      } else {
        state.update(Result(sessionId, false, current.count + events.length))
      }

      events.foreach(e => {
        println(s"EV ${e.getAs[String]("sessionId")}")
      })

      println(s"US ${state.get} ${events.length}")
      state.get
    }
  }
}
