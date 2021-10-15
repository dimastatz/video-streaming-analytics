package dimastatz.flumenz.utilities

import scala.util._

/*
Parsing Edgecast logs. For fields description see:
https://docs.edgecast.com/cdn/Content/RTLD/Log-Fields.htm
 */
object Edgecast {
  implicit class Path(path: String) {
    def getOwnerId: Try[String] = getPathPart()
    def getBeamId: Try[String] = getPathPart(1)

    def getPathPart(index: Int = 0): Try[String] =
      Try {
        val pattern = "/[0-9a-fA-F]{32}/[0-9a-fA-F]{32}/".r
        val groups = pattern.findAllIn(path.split("/slices")(1))
        groups.toList.head.split("/")(index + 1)
      }
  }
}
