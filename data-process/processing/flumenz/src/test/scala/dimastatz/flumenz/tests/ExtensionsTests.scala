package dimastatz.flumenz.tests

import org.scalatest.funsuite.AnyFunSuite
import dimastatz.flumenz.tests.utils.SparkTest
import dimastatz.flumenz.utilities.KafkaExtensions
import org.apache.kafka.clients.consumer.KafkaConsumer

class ExtensionsTests extends AnyFunSuite with SparkTest {
  private val session = getSession()

  test(testName = "testKafkaExtensions") {
    val mapper = KafkaExtensions.createScalaObjectMapper
    assert(mapper.canSerialize(classOf[Map[String, Map[String, Int]]]))
    val consumer = KafkaExtensions.createKafkaConsumer("localhost:9092", "groupid")
    assert(consumer.isInstanceOf[KafkaConsumer[String, String]])
    val result = KafkaExtensions.commitOffsets(mapper, "", "dummy", consumer)
    assert(result.isFailure)
  }

  test(testName = "slated join test") {
    import session.implicits._
    import dimastatz.flumenz.utilities.Extensions._

    val df1 = Seq(("1", "1"), ("1", "2"), ("3", "1")).toDF("beam_id", "slice_id")
    val df2 = Seq(("1", "[1, 2, 3]"), ("2", "[1, 2, 3, 4]")).toDF("beam_id", "slice_id_range")
    val df3 = Seq(("1", "[1, 2, 3]"), ("1", "[1, 2, 3, 4]")).toDF("beam_id", "slice_id_range")
    val df4 = Seq(("5", "[1, 2, 3]"), ("6", "[1, 2, 3, 4]")).toDF("beam_id", "slice_id_range")

    var res1 = df1.join(df2, df1("beam_id") === df2("beam_id"), "left")
    var res2 = df1.saltedJoin(df2, df1("beam_id") === df2("beam_id"), "left", 3)
    assert(res1.count() == 3)
    assert(res1.equalsTo(res2))

    res1 = df1.join(df3, df1("beam_id") === df3("beam_id"), "left")
    res2 = df1.saltedJoin(df3, df1("beam_id") === df3("beam_id"), "left", 3)
    assert(res1.count() == 5)
    assert(res1.equalsTo(res2))

    res1 = df1.join(df3, df1("beam_id") === df3("beam_id"), "inner")
    res2 = df1.saltedJoin(df3, df1("beam_id") === df3("beam_id"), "inner", 3)
    assert(res1.count() == 4)
    assert(res1.equalsTo(res2))

    res1 = df1.join(df4, df1("beam_id") === df4("beam_id"), "left")
    res2 = df1.saltedJoin(df4, df1("beam_id") === df4("beam_id"), "left", 3)
    assert(res1.count() == 3)
    assert(res1.equalsTo(res2))

    res1 = df1.join(df4, df1("beam_id") === df4("beam_id"), "inner")
    res2 = df1.saltedJoin(df4, df1("beam_id") === df4("beam_id"), "inner", 3)
    assert(res1.count() == 0)
    assert(res1.equalsTo(res2))
  }
}
