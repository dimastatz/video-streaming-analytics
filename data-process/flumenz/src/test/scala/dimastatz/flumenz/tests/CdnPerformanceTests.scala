package dimastatz.flumenz.tests

import org.apache.spark.sql.SparkSession
import org.scalatest.funsuite.AnyFunSuite
import dimastatz.flumenz.cdnquality.CdnQualityPipeline

class CdnPerformanceTests extends AnyFunSuite with SparkTest {
  val session: SparkSession = getSession("testCdnQualityFlow")

  test(testName = "testCdnQualityFlow") {
    val res = getClass.getResource("/cdn_log.json")
    val df = session.read.json(res.getPath)
    df.show()
    val query = CdnQualityPipeline.query(df)
    query.show(false)
  }
}
