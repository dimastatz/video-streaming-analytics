package dimastatz.flumenz.tests

import org.scalatest.funsuite.AnyFunSuite
import com.typesafe.config.ConfigException
import dimastatz.flumenz.tests.utils.SparkTest

class ConfigTests extends AnyFunSuite with SparkTest {
  test("testConfFallback") {
    assertThrows[ConfigException.Missing](getConfig.getString("conf.spark.assembly"))
    assert(getConfigWithFallBack.getString("conf.spark.assembly") == "/opt/spark/jars/flumenz-assembly-0.2.jar")
  }
}
