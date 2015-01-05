package board.controller

import java.io.PrintWriter

import scala.util.control.NonFatal

import board.impl.Post
import org.json4s._
import org.json4s.native.Serialization.{read, write}

import simplehttpserver.impl._
import simplehttpserver.util.EasyEmit
import simplehttpserver.util.implicits.Implicit._
import simplehttpserver.util.Security._

object BoardController extends EasyEmit {
  type Action = HttpRequest => HttpResponse
  private val path2Data = "./private/data.json"
  private val builder = new HtmlBuilder()
  private val base = getStringFromResources("base.html")
  private val buildWithBase = builder.buildHtml(base.get) _
  private val postBase = getStringFromResources("postBase.html")
  private val buildWithPostBase = builder.buildHtml(postBase.get) _
  private val boardBase = getStringFromResources("boardBase.html")
  private val buildWithBoardBase = builder.buildHtml(boardBase.get) _

  def boardG: Action = req => {
    val contentType = "Content-Type" -> html.contentType
    val title = "Board"
    val head = """<script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
                 |<script src="./js/script.js" type="text/javascript"></script>
                 |<link href="./css/style.css" rel="stylesheet" type="text/css">""".stripMargin

    implicit val formats = DefaultFormats
    val posts = getStringFromFile(path2Data) match {
      case Some(json) => read[List[Post]](json)
      case None => throw new Exception("route file not found")
    }

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

  //data.json取得, postを解析して追加, そこからresponse作成しつつdata.json更新
  def boardP: Action = req => {
    val contentType = "Content-Type" -> html.contentType
    val title = "Board"
    val head = """<script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
                 |<script src="./js/script.js" type="text/javascript"></script>
                 |<link href="./css/style.css" rel="stylesheet" type="text/css">""".stripMargin

    implicit val formats = DefaultFormats
    val posts = getStringFromFile(path2Data) match {
      case Some(json) => read[List[Post]](json)
      case None => throw new Exception("route file not found")
    }

    val newPosts = {
      val name = req.body.getOrElse("name", "")
      val date = Some(new java.util.Date())
      val content = req.body.getOrElse("content", "")
      val imgs = req.body.get("imgs") match {
        case Some(x) => List() //TODO: x:String(=json array) を List[String] に変換
        case None => List()
      }
      posts :+ Post(
        true,
        escape(name) getOrElse "",
        date,
        escape(content) getOrElse "",
        imgs map {
          escape(_) getOrElse ""
        })
    }
    val added = write(newPosts)

    val pw = new PrintWriter(path2Data)
    try {
      pw.print(added)

      val formed =
        newPosts
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
    } catch {
      case NonFatal(ex) =>
        println(ex)
        emitError(req)(InternalServerError)
    } finally {
      pw.close()
    }
  }
}
