# Problem Scope
Design and implement the Cached Data Source to improve the performance of slow data sources. The Cached Data Source should support automatic expiration.

- Example:
    A certain spark application performs the data enrichment step by accessing MongoDB. The Spark application and MongoDB are running in different regions. The latency between two is as high as 200ms. The Cached Data source should access MongoDB only once for each value. The following code demonstrates an application flow. 

    ```scala
    val getStateUdf = udf(getState _)

     df
      .select("timestamp", "ip", "status_code")
      .withColumn("state", getStateUdf(col("ip"))
      .drop("ip")
      .withWatermark("timestamp", "1 minutes")
      .groupBy(window(col("timestamp"), "1 minutes"), col("state"))
      .agg(
        count("status_code").as("total"),
        count(when(col("status_code") > 299, 1)).as("http_error")
      )

getStateUdf works slow when accessing MongoDB directly. Suggest a solution for the Cached Data Source.


# Solution

```scala
import scala.util._
import scala.concurrent.duration._
import com.github.blemale.scaffeine._

case class Notification(
    hit: Option[Boolean],
    readFailure: Option[Throwable] = None
)

case class CachedSourceParams[Q, R](
    expireHr: Int,
    cacheSize: Int,
    notifier: Notification => Unit,
    readSource: Q => R,
    default: R
)

class CachedSource[Q, R](params: CachedSourceParams[Q, R], cache: Option[Cache[Q, R]]) {
  private val innerCache = cache match {
    case Some(c) => c
    case None =>
      Scaffeine()
        .recordStats()
        .expireAfterAccess(params.expireHr.hour)
        .maximumSize(params.cacheSize)
        .build[Q, R]()
  }

  def readCache(query: Q): R = {
    innerCache.getIfPresent(query) match {
      case Some(x) =>
        params.notifier(Notification(Some(true)))
        x
      case None =>
        params.notifier(Notification(Some(false)))
        innerCache.put(query, tryReadFromSource(query))
        innerCache.getIfPresent(query).get
    }
  }

  def tryReadFromSource(query: Q): R = {
    Try(params.readSource(query)) match {
      case Success(v) => v
      case Failure(x) =>
        params.notifier(Notification(None, Some(x)))
        params.default
    }
  }
}
      