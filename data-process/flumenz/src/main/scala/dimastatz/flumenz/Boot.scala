package dimastatz.flumenz

import scala.util._
import org.apache.log4j._
import com.typesafe.config._
import org.apache.spark.sql.SparkSession
import scala.collection.JavaConverters._

object Boot {
  private lazy val log = LogManager.getLogger(Boot.getClass)

  def main(args: Array[String]): Unit = {
    log.info(s"starting streaming app ${args.mkString(",")}")
    val name = s"${resolveEnv(args)}.conf"
    val conf = ConfigFactory.load(name)

    Try(startStream(conf, args.head)) match {
      case Success(_) =>
        log.info("Legion Streaming completed")
      case Failure(e) =>
        throw e
    }
  }

  def resolveEnv(args: Array[String]): String = {
    args.length match {
      case 0 => "local"
      case _ => args.head
    }
  }

  def startStream(conf: Config, head: String): Unit = {
    import dimastatz.flumenz.utilities.Extensions._

    log.info("creating spark session")
    val session = SparkSession.builder.getOrCreate()

    log.info("starting kafka stream")
    val kafkaConf = conf.getConfig("conf.spark.kafka")
    val df = session.readStream
      .format("kafka")
      .option("maxOffsetsPerTrigger", kafkaConf.getInt("maxOffsetsPerTrigger"))
      .option("subscribe", kafkaConf.getStringList("topics").asScala.mkString(","))
      .option("kafka.bootstrap.servers", kafkaConf.getStringList("brokers").asScala.mkString(","))
      .load()
      .getKafkaLabels

    log.info("adding event listener")

    Pipelines
      .create()
      .foreach(p => {
        p.query(df)
          .writeStream
          .format("parquet")
          .option("compression", "snappy")
          .partitionBy(p.getPartitions: _*)
          .outputMode("append")
          .queryName(s"${p.getName}_${p.getName}")
          .option("path", s"${kafkaConf.getString("sink")}/${p.getName}/")
          .option("checkpointLocation", s"${kafkaConf.getString("checkpoint")}/${p.getName}/")
          .start()
      })

    session.streams.awaitAnyTermination()
  }
}
