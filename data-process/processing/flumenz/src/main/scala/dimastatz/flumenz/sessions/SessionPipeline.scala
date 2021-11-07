package dimastatz.flumenz.sessions

import java.sql._
import org.apache.spark.sql._
import dimastatz.flumenz.Pipeline
import org.apache.spark.sql.streaming._

class SessionPipeline(session: SparkSession, watermark: Int) extends Pipeline {
  case class Event(sessionId: String, eventType: String, ts: Timestamp)
  case class Session(
      sessionId: String,
      start: Timestamp,
      close: Timestamp,
      duration: Int,
      events: Int,
      close_dt: String
  )

  override def getName: String = "SessionsPipeline"

  override def getPartitions: Seq[String] = {
    List("close_dt")
  }

  override def query(df: DataFrame): DataFrame = {
    import session.implicits._
    df
      .select("sessionId", "eventType", "ts")
      .as[Event]
      .withWatermark("timestamp", delayThreshold = s"$watermark minutes")
      .groupByKey(_.sessionId)
      .mapGroupsWithState(timeoutConf = GroupStateTimeout.EventTimeTimeout())(process)
      .toDF()
  }

  private def process(sessionId: String, events: Iterator[Event], state: GroupState[Session]): Session = {
    val ts = new Timestamp(System.currentTimeMillis())
    val session = state.getOption.getOrElse(Session(sessionId, ts, ts, 0, 0, ""))

    if (state.hasTimedOut) {
      state.remove()
      session
    } else {
      val updatedSession = events.toList.foldLeft(session)((s, e) =>
        e.eventType match {
          case "sessionOpen" =>
            Session(s.sessionId, e.ts, s.close, getDuration(s.close, e.ts), s.events + 1, s.close_dt)
          case "sessionProgress" =>
            Session(s.sessionId, s.start, s.close, s.duration, s.events + 1, s.close_dt)
          case "sessionClose" =>
            Session(s.sessionId, s.start, e.ts, getDuration(s.close, e.ts), s.events + 1, convertTs(e.ts))
        }
      )

      state.update(updatedSession)
      updatedSession
    }
  }

  def getDuration(start: Timestamp, end: Timestamp): Int = {
    (end.getTime() - start.getTime() / 1000).asInstanceOf[Int]
  }

  def convertTs(ts: Timestamp, pattern: String = ""): String = {
    import dimastatz.flumenz.utilities.Extensions._
    ts.convertTimestamp(pattern)
  }
}
