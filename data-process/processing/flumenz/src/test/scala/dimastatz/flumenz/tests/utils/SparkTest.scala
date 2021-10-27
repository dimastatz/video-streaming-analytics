package dimastatz.flumenz.tests.utils

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.SparkSession

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

trait SparkTest {

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

  def getConfigWithFallBack: Config = {
    ConfigFactory
      .load(s"test.conf")
      .withFallback(ConfigFactory.load("base.conf"))
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
