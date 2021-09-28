package dimastatz.flumenz

import scala.util._
import org.apache.log4j._
import com.typesafe.config._

object Boot {
  private lazy val log = LogManager.getLogger(Boot.getClass)

  def main(args: Array[String]): Unit = {
    log.info(s"starting streaming app ${args.mkString(",")}")
    val name = s"${resolveEnv(args)}.conf"
    val conf = ConfigFactory.load(name)

    Try(startStream(conf, args.head)) match {
      case Success(_) =>
        log.info("Legion Streaming completed")
      case Failure(e) =>
        throw e
    }
  }

  def resolveEnv(args: Array[String]): String = {
    args.length match {
      case 0 => "local"
      case _ => args.head
    }
  }

  def startStream(conf: Config, head: String): Unit = {
    log.info("TODO: stream is not implemented")
  }
}
