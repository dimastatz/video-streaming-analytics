package dimastatz.flumenz

import org.apache.log4j.LogManager

object Boot {
  private lazy val log = LogManager.getLogger(Boot.getClass)

  def main(args: Array[String]): Unit = {
    log.info("running main")
  }
}
