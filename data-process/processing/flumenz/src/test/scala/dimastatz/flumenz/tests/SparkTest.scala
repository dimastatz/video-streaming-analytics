package dimastatz.flumenz.tests

import java.sql._
import java.time._
import com.typesafe.config._
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.format.DateTimeFormatter

trait SparkTest {
  private val defaultPort = 9999

  org.slf4j.LoggerFactory
    .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    .asInstanceOf[ch.qos.logback.classic.Logger]
    .setLevel(ch.qos.logback.classic.Level.WARN)

  def getSession(name: String = "flumenz-test"): SparkSession = {
    val session = SparkSession.builder
      .master("local[1]")
      .appName(name)
      .getOrCreate()

    session.sparkContext.setLogLevel("WARN")
    session
  }

  def getConfig: Config = {
    ConfigFactory.load(s"test.conf")
  }

  def getSocketStream(session: SparkSession, port: Int = defaultPort): DataFrame = {
    session
      .readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", port)
      .load()
  }
  
  def toTimestamp(ts: String): Timestamp = {
    new Timestamp(
      LocalDateTime
        .parse(ts, DateTimeFormatter.ISO_DATE_TIME)
        .atZone(ZoneId.of("UTC"))
        .toInstant
        .toEpochMilli
    )
  }
}
