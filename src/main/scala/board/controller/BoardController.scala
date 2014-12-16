package board.controller

import simplehttpserver.impl._
import simplehttpserver.util.Util._
import simplehttpserver.util.Implicit._


object BoardController {
  type Action = HttpRequest => HttpResponse
  private val builder = new HtmlBuilder()
  private val base = getStringFromResources("base.html")
  private val buildWithBase = builder.buildHtml(base.get) _
  private val postBase = getStringFromResources("postBase.html")
  private val buildWithPostBase = builder.buildHtml(postBase.get) _

  def board: Action = req => {
    val contentType = "Content-Type" -> html.contentType
    val title = "掲示板"
    val head =
      """
        |<script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
        |<link href="./css/style.css" rel="stylesheet" type="text/css">
      """.stripMargin





    val bodyBase = """"""
    val body = bodyBase

    HttpResponse(req)(
      Ok,
      header = Map(contentType),
      body = buildWithBase(Seq(
        "<<title>>" -> title,
        "<<head>>" -> head,
        "<<body>>" -> body))
    )
  }

  def postPage: Action = req => {
    val contentType = "Content-Type" -> html.contentType

    val title = "ポストしてみよう"
    val head = ""
    val body = """<form action="/postTest" method="post">
                 |<p>お名前：<input type="text" name="namae" value="" size="20" /></p>
                 |<p>OS：
                 |<input type="radio" name="OS" value="win" checked="checked" /> Windows
                 |<input type="radio" name="OS" value="mac" /> Machintosh
                 |<input type="radio" name="OS" value="unix" /> Unix
                 |</p>
                 |<p><input type="submit" name="submit" value="送信" /></p>
                 |</form>
                 |</form>""".stripMargin

    HttpResponse(req)(
      Ok,
      header = Map(contentType),
      body = buildWithBase(Seq(
        "<<title>>" -> title,
        "<<head>>" -> head,
        "<<body>>" -> body)))
  }
}
