package dimastatz.flumenz.tests

import dimastatz.flumenz.tests.utils.SparkTest
import org.scalatest.funsuite.AnyFunSuite
import dimastatz.flumenz.utilities.KafkaExtensions
import org.apache.kafka.clients.consumer.KafkaConsumer

class KafkaExtensionsTests extends AnyFunSuite with SparkTest {
  test(testName = "testKafkaExtensions") {
    val mapper = KafkaExtensions.createScalaObjectMapper
    assert(mapper.canSerialize(classOf[Map[String, Map[String, Int]]]))
    val consumer = KafkaExtensions.createKafkaConsumer("localhost:9092", "groupid")
    assert(consumer.isInstanceOf[KafkaConsumer[String, String]])
    val result = KafkaExtensions.commitOffsets(mapper, "", "dummy", consumer)
    assert(result.isFailure)
  }
}
