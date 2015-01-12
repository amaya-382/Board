package board

import simplehttpframework.HttpServer

object Starter {
  def main(args: Array[String]) {
    val server = new HttpServer(8080)
    server.start()
  }
}
