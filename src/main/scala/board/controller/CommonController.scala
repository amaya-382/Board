package board.controller

import java.io.File

import simplehttpframework.impl._
import simplehttpframework.util.EasyEmit
import simplehttpframework.util.implicits.Implicit._


object CommonController extends EasyEmit {
  type Action = HttpRequest => HttpResponse
  private val builder = new HtmlBuilder()
  private val base = getStringFromResources("base.html")
  private val buildWithBase = builder.buildHtml(base getOrElse "") _

  def root: Action = req => {
    val contentType = "Content-Type" -> html.contentType

    val title = "Welcome!!"
    val body =
      """<h1>Welcome to Simple Http Server!</h1>
        |<hr>
        |<a href="./board">掲示板へ</a>""".stripMargin

    HttpResponse(req)(
      Ok,
      header = Map(contentType),
      body = buildWithBase(Seq(
        "title" -> title,
        "head" -> "",
        "body" -> body
      )))
  }

  def echo: Action = req => {
    val contentType = "Content-Type" -> html.contentType

    val title = "echo system"
    val body = s"<p>${req.req}</p><p>${req.header.mkString("<br>")}</p>"

    HttpResponse(req)(
      Ok,
      header = Map(contentType),
      body = buildWithBase(Seq(
        "title" -> title,
        "head" -> "",
        "body" -> body)))
  }

  def asset: Action = req => {
    findAsset("./public" + req.req._2) match {
      case Some(file) =>
        println("asset found!")
        val cont = getByteArrayFromFile(file)

        HttpResponse(req)(Ok, body = cont)
      case None =>
        emitError(req)(NotFound)
    }
  }

  private def findAsset(path: String): Option[File] = {
    val file = new File(path)
    if (file.exists && file.isFile)
      Some(file)
    else
      None
  }
}
