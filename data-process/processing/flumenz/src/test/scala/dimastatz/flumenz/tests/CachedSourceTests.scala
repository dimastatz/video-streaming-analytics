package dimastatz.flumenz.tests

import com.github.blemale.scaffeine.Cache
import dimastatz.flumenz.cachedsources._
import org.scalatest.funsuite.AnyFunSuite

class CachedSourceTests extends AnyFunSuite {

  test("testCachedSourceTrait") {
    class TestSource extends CachedSource[Int, String] {
      override val defaultResult: String = "empty"
      var list: List[Notification] = List[Notification]()
      val cache: Cache[Int, String] = createCache(1, 3)

      def read(query: Int): String = {
        readCache(cache)(query)
      }

      override def readFromSource(query: Int): String = {
        if (query < 3) {
          query.toString
        } else {
          throw new Exception(s"key not found $query")
        }
      }

      override def notify(message: Notification): Unit = {
        list ::= message
      }
    }

    val test = new TestSource()
    assert(test.read(1) == "1")
    assert(!test.list.last.hit.get)
  }
}
