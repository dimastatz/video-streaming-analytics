package dimastatz.flumenz.sessions

import java.sql._
import org.apache.spark.sql._
import dimastatz.flumenz.Pipeline
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming._

case class Event(
    sessionId: String,
    eventType: String,
    timestamp: Timestamp
)

case class Session(
    sessionId: String,
    start: Timestamp,
    close: Timestamp,
    duration: Int,
    events: Int,
    close_dt: String
)

class SessionPipeline(session: SparkSession, watermark: Int) extends Pipeline with Serializable {
  override def getName: String = "SessionsPipeline"

  override def getPartitions: Seq[String] = {
    List("close_dt")
  }

  override def query(df: DataFrame): DataFrame = {
    val resultDf = df
      .select("timestamp", "value", "topic")
      .filter(col("topic") === "cdnlogs")
      .select("sessionId", "eventType", "ts")
    aggregate(resultDf, watermark)
  }

  def aggregate(df: DataFrame, watermark: Int): DataFrame = {
    import session.implicits._
    df
      .as[Event]
      .withWatermark("timestamp", delayThreshold = s"$watermark minutes")
      .groupByKey(_.sessionId)
      .mapGroupsWithState(timeoutConf = GroupStateTimeout.EventTimeTimeout())(process)
      .toDF()
  }

  private def process(sessionId: String, events: Iterator[Event], state: GroupState[Session]): Session = {
    val ts = new Timestamp(0)
    val session = state.getOption.getOrElse(Session(sessionId, ts, ts, 0, 0, ""))

    if (state.hasTimedOut) {
      state.remove()
      session
    } else {
      val eventsList = events.toList
      println(eventsList)

      val updatedSession = events.toList.foldLeft(session)((s, e) =>
        e.eventType match {
          case "sessionOpen" =>
            Session(s.sessionId, e.timestamp, s.close, getDuration(s.close, e.timestamp), s.events + 1, s.close_dt)
          case "sessionProgress" =>
            Session(s.sessionId, s.start, s.close, s.duration, s.events + 1, s.close_dt)
          case "sessionClose" =>
            Session(
              s.sessionId,
              s.start,
              e.timestamp,
              getDuration(s.close, e.timestamp),
              s.events + 1,
              convertTs(e.timestamp)
            )
        }
      )
      state.update(updatedSession)
      updatedSession
    }
  }

  def getDuration(start: Timestamp, end: Timestamp): Int = {
    (end.getTime - start.getTime / 1000).asInstanceOf[Int]
  }

  def convertTs(ts: Timestamp, pattern: String = ""): String = {
    import dimastatz.flumenz.utilities.Extensions._
    ts.convertTimestamp(pattern)
  }
}
