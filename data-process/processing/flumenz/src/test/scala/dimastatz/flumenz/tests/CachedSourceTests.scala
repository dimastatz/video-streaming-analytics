package dimastatz.flumenz.tests

import dimastatz.flumenz.cachedsources._
import org.scalatest.funsuite.AnyFunSuite

class CachedSourceTests extends AnyFunSuite {
  test("testCachedSourceTrait") {
    var notifications = List[Notification]()
    val notifier = (n: Notification) => {
      notifications ++= List(n)
    }

    val reader = (k: Int) => {
      if (k == 1) {
        throw new Exception(s"key not found $k")
      } else {
        k.toString
      }
    }

    val params = CachedSourceParams(1, 3, notifier, reader, "key_not_exists")
    val cachedSource = new CachedSource[Int, String](params, None)

    assert(cachedSource.readCache(1) == "key_not_exists")
    assert(notifications.length == 2)
    assert(notifications.last.readFailure.nonEmpty)

    assert(cachedSource.readCache(0) == "0")
    assert(notifications.length == 3)
    assert(!notifications.last.hit.get)

    assert(cachedSource.readCache(0) == "0")
    assert(notifications.length == 4)
    assert(notifications.last.hit.get)
  }

}
