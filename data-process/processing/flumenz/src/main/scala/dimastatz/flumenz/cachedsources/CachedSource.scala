package dimastatz.flumenz.cachedsources

import scala.util._
import scala.concurrent.duration._
import com.github.blemale.scaffeine._

case class Notification(hit: Option[Boolean], readFailure: Option[Throwable] = None)

trait CachedSource[Q, R] {
  def createCache(expireHr: Int, size: Int): Cache[Q, R] = {
    Scaffeine().recordStats().expireAfterAccess(expireHr.hour).maximumSize(size).build[Q, R]()
  }

  def readCache(cache: Cache[Q, R])(query: Q): R = {
    cache.getIfPresent(query) match {
      case Some(x) =>
        notify(Notification(Some(true)))
        x
      case None =>
        notify(Notification(Some(false)))
        cache.put(query, tryReadFromSource(query))
        cache.getIfPresent(query).get
    }
  }

  def tryReadFromSource(query: Q): R = {
    Try(readFromSource(query)) match {
      case Success(v) => v
      case Failure(x) => defaultResult
    }
  }

  val defaultResult: R
  def readFromSource(query: Q): R
  def notify(message: Notification): Unit
}
