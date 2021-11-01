package dimastatz.flumenz.cachedsources

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
