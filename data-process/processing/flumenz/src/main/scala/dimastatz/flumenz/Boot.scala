package dimastatz.flumenz

import scala.util._
import org.apache.log4j._
import com.typesafe.config._
import dimastatz.flumenz.utilities.KafkaExtensions
import dimastatz.flumenz.utilities.KafkaExtensions.createKafkaConsumer

import scala.collection.mutable
import scala.collection.JavaConverters._
import org.apache.spark.sql.SparkSession
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.spark.sql.streaming.StreamingQueryListener
import org.apache.spark.sql.streaming.StreamingQueryListener._

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
    val sparkConf = conf.getConfig("conf.spark")

    Pipelines
      .create(kafkaConf.getStringList("topics").asScala.toList)
      .foreach(p => {
        p.query(df)
          .writeStream
          .format("parquet")
          .option("compression", "snappy")
          .partitionBy(p.getPartitions: _*)
          .outputMode("append")
          .queryName(s"${p.getName}_${p.getName}")
          .option("path", s"${sparkConf.getString("sink")}/${p.getName}/")
          .option("checkpointLocation", s"${sparkConf.getString("checkpoint")}/${p.getName}/")
          .start()
      })

    session.streams.awaitAnyTermination()
  }

  class EventCollector(servers: String, topic: String) extends StreamingQueryListener {
    private val oMap = KafkaExtensions.createScalaObjectMapper
    private val cMap = mutable.Map[String, KafkaConsumer[String, String]]()

    override def onQueryProgress(event: QueryProgressEvent): Unit = {
      import event.progress._
      cMap.getOrElseUpdate(name, createKafkaConsumer(servers, name))

      KafkaExtensions.commitOffsets(oMap, sources.head.endOffset, topic, cMap(name)) match {
        case Success(_) => log.info(s"QueryProgress committed $json offsets")
        case Failure(e) => log.error(s"QueryProgress failed $json $e to commit offsets")
      }
    }

    override def onQueryStarted(event: QueryStartedEvent): Unit = {
      log.info(s"QueryStarted ${event.name} ${event.timestamp}")
    }

    override def onQueryTerminated(event: QueryTerminatedEvent): Unit = {
      log.error(s"QueryTerminated ${event.id} ${event.exception}")
    }
  }
}
