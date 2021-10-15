package dimastatz.flumenz.utilities

import scala.util._

object Edgecast {
  implicit class Path(path: String) {
    def getBeamId: Try[String] = getPathPart(1)
    def getOwnerId: Try[String] = getPathPart(2)

    def getPathPart(index: Int): Try[String] = {
      (path.contains("slices"), path.split("/")) match {
        case (true, x)  => Try(x.reverse(index))
        case (false, _) => Failure(new Exception("Slice not found"))
      }
    }
  }
}
