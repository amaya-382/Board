package board.controller

import board.impl.Post
import org.json4s._
import org.json4s.native.JsonMethods

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
  private val boardBase = getStringFromResources("boardBase.html")
  private val buildWithBoardBase = builder.buildHtml(boardBase.get) _

  def board: Action = req => {
    val contentType = "Content-Type" -> html.contentType
    val title = "Board"
    val head = """<script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
                 |<script src="./js/script.js" type="text/javascript"></script>
                 |<link href="./css/style.css" rel="stylesheet" type="text/css">""".stripMargin

    implicit val formats = DefaultFormats
    val json = getStringFromFile("./private/data.json") match {
      case Some(d) => JsonMethods.parse(d)
      case None => throw new Exception("route file not found")
    }
    val posts = json.extract[List[Post]]

    val formed =
      posts
        .withFilter(_.enabled)
        .map(post => {
        val imgs = post.imgs
          .map(i => s"""<img src="$i" class="img" />""")
          .mkString
        buildWithPostBase(Seq(
          "name" -> post.name,
          "date" -> post.date.map(_.formatted("%tF %<tT")).mkString,
          "content" -> post.content,
          "imgs" -> imgs
        ))
      })

    val body = buildWithBoardBase(Seq(
      "posts" -> formed.mkString
    ))

    HttpResponse(req)(
      Ok,
      header = Map(contentType),
      body = buildWithBase(Seq(
        "title" -> title,
        "head" -> head,
        "body" -> body))
    )
  }
}
