package dimastatz.flumenz.utilities

import scala.util._
import java.util.Properties
import com.fasterxml.jackson.databind._
import org.apache.kafka.clients.consumer._
import com.fasterxml.jackson.module.scala._
import org.apache.kafka.common.TopicPartition

object KafkaExtensions {
  def createScalaObjectMapper: ObjectMapper = {
    import DeserializationFeature._
    val objectMapper = new ObjectMapper()
    objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    objectMapper.registerModule(DefaultScalaModule)
    objectMapper
  }

  def createKafkaConsumer(servers: String, groupId: String): KafkaConsumer[String, String] = {
    val props = new Properties()
    props.put("bootstrap.servers", servers)
    props.put("group.id", groupId)
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    new KafkaConsumer[String, String](props)
  }

  def commitOffsets[T](map: ObjectMapper, data: String, topic: String, csm: KafkaConsumer[T, T]): Try[Unit] = {
    Try {
      val topicPartitionMap = new java.util.HashMap[TopicPartition, OffsetAndMetadata]()
      val topicsInfo = map.readValue(data, classOf[Map[String, Map[String, Int]]])
      val offsetsInfo = topicsInfo(topic)

      offsetsInfo.keys.foreach(partitions => {
        val tp = new TopicPartition(topic, partitions.toInt)
        val offset = new OffsetAndMetadata(offsetsInfo(partitions).toLong)
        topicPartitionMap.put(tp, offset)
      })
      csm.commitSync(topicPartitionMap)
    }
  }
}
