package board.controller

import board.entity._

import org.json4s._
import org.json4s.native.Serialization.{read, write}

import simplehttpserver.impl._
import simplehttpserver.util.{Security, EasyEmit}
import simplehttpserver.util.implicits.Implicit._
import simplehttpserver.util.Security._
import simplehttpserver.util.Common._


object BoardController extends EasyEmit {
  type Action = HttpRequest => HttpResponse
  implicit private val formats = DefaultFormats

  private val path2PostData = "./private/post.json"
  private val path2UserData = "./private/user.json"

  private val builder = new HtmlBuilder()
  private val base = getStringFromResources("base.html")
  private val buildWithBase = builder.buildHtml(base getOrElse "") _
  private val signUpBase = getStringFromResources("signUpBase.html")
  //  private val buildWithSignUpBase = builder.buildHtml(signUpBase getOrElse "") _
  private val signInBase = getStringFromResources("signInBase.html")
  //  private val buildWithLoginBase = builder.buildHtml(signInBase getOrElse "") _
  private val postBase = getStringFromResources("postBase.html")
  private val buildWithPostBase = builder.buildHtml(postBase getOrElse "") _
  private val timeLineBase = getStringFromResources("timeLineBase.html")
  private val buildWithTimeLineBase = builder.buildHtml(timeLineBase getOrElse "") _
  private val boardBase = getStringFromResources("boardBase.html")
  private val buildWithBoardBase = builder.buildHtml(boardBase getOrElse "") _

  private val head4sign = """<script src="/js/jquery-1.11.2.min.js" type="text/javascript"></script>
                            |<script src="/js/jquery.pjax.min.js" type="text/javascript"></script>
                            |<link href='http://fonts.googleapis.com/css?family=Rock+Salt' rel='stylesheet' type='text/css'>
                            |<link href='http://fonts.googleapis.com/css?family=Oswald:700' rel='stylesheet' type='text/css'>
                            |<link href='http://fonts.googleapis.com/css?family=Open+Sans+Condensed:300' rel='stylesheet' type='text/css'>
                            |<link href="/css/board.css" rel="stylesheet" type="text/css">
                            |<link href="/css/board_sign.css" rel="stylesheet" type="text/css">
                            | """.stripMargin


  def signUpPage: Action = req => {
    val contentType = "Content-Type" -> html.contentType
    val title = "Sing up"

    HttpResponse(req)(
      status = Ok,
      header = Map(contentType),
      body = buildWithBase(Seq(
        "title" -> title,
        "head" -> head4sign,
        "body" -> (signUpBase getOrElse "")
      )))
  }

  def signInPage: Action = req => {
    val contentType = "Content-Type" -> html.contentType
    val title = "Login"

    HttpResponse(req)(
      status = Ok,
      header = Map(contentType),
      body = buildWithBase(Seq(
        "title" -> title,
        "head" -> head4sign,
        "body" -> (signInBase getOrElse "")
      )))
  }

  def signedIn: Action = req => {
    val contentType = "Content-Type" -> html.contentType
    val title = "Board"
    val head = """<script src="/js/jquery-1.11.2.min.js" type="text/javascript"></script>
                 |<script src="/js/jquery.pjax.min.js" type="text/javascript"></script>
                 |<script src="/js/script.js" type="text/javascript"></script>
                 |<link href='http://fonts.googleapis.com/css?family=Oswald:700' rel='stylesheet' type='text/css'>
                 |<link href='http://fonts.googleapis.com/css?family=Open+Sans+Condensed:300' rel='stylesheet' type='text/css'>
                 |<link href="/css/board.css" rel="stylesheet" type="text/css">
                 |<link href="/css/board_timeLine.css" rel="stylesheet" type="text/css">""".stripMargin

    val posts = getStringFromFile(path2PostData) match {
      case Some(json) => read[List[Post]](json)
      case None => throw new Exception("route file not found")
    }

    val userList = getUsers

    val user = req.session flatMap {
      s => userList find (_.id == s.id)
    }

    val formed =
      posts
        .withFilter(_.enabled)
        .map(post => {
        buildWithPostBase(Seq(
          "isOwn" -> (if (user.exists(_.id == post.id)) "own" else ""),
          "name" -> userList.find(_.id == post.id).map(_.name).getOrElse("x"),
          "date" -> post.date.map(_.formatted("%tF %<tT")).mkString,
          "content" -> post.content.replaceAll("&lt;br&gt;", "<br>")
        ))
      })

    val cookieHeader = {
      val sessionId = req.session.map(_.sessionId).getOrElse("")
      "Set-Cookie" -> s"SESSIONID=$sessionId; path=/board;"
    }

    val timeLine = buildWithTimeLineBase(Seq(
      "posts" -> formed.mkString
    ))

    val body = buildWithBoardBase(Seq(
      "userName" -> user.map(_.name).getOrElse("x"),
      "timeLine" -> timeLine
    ))

    HttpResponse(req)(
      Ok,
      header = Map(contentType, cookieHeader),
      body = buildWithBase(Seq(
        "title" -> title,
        "head" -> head,
        "body" -> body))
    )
  }

  def boardPage: Action = req => {
    req.session match {
      case Some(s) if s.isVaild =>
        signedIn(req)
      case _ =>
        signInPage(req)
    }
  }

  def redirect2Board: Action = req => {
    val locationHeader = "Location" -> "/board"

    HttpResponse(req)(
      status = MovedPermanently,
      header = Map(locationHeader),
      body = "")
  }


  def signUp: Action = req => {
    (for {
      id <- req.body.get("id").flatMap(dropCtrlChar)
      pwd <- req.body.get("password").flatMap(dropCtrlChar)
      repwd <- req.body.get("reinput").flatMap(dropCtrlChar)
      name <- req.body.get("name")
    } yield {
      //id, pwd に使えない文字が入っていた場合は再度signUpPageへ
      if (!validate4Id(id))
        signUpPage(req) //TODO:msg表示
      else if (!validate4Pwd(pwd, repwd))
        signUpPage(req) //TODO:msg表示
      else {
        val salt = Security.hashBySHA384(id)
        val hashedPwd = Security.hashBySHA384(pwd + salt)
        val newUsers = getUsers :+
          User(id, hashedPwd, escape(name) getOrElse id)

        writeWithResult(path2UserData)(pw => {
          pw.print(write(newUsers))
          req.refreshSession(false)
          pw.flush()
          boardPage(req)
        })(ex => {
          emitError(req)(InternalServerError)
        })
      }
    }) getOrElse emitError(req)(InternalServerError)
  }

  def signIn: Action = req => {
    (for {
      id <- req.body.get("id")
      pwd <- req.body.get("password")
    } yield {
      val users = getUsers

      val salt = Security.hashBySHA384(id)
      val hashedPwd = Security.hashBySHA384(pwd + salt)

      users.find(user => user.id == id && user.hashedPwd == hashedPwd) match {
        case Some(user) =>
          req.refreshSession(false)
          signedIn(req)
        case None =>
          signInPage(req)
      }
    }) getOrElse emitError(req)(InternalServerError)
  }

  def signOut: Action = req => {
    val cookieHeader = "Set-Cookie" -> "SESSIONID=; expires=Thu, 1-Jan-1970 00:00:00 GMT; path=/board;"
    val locationHeader = "Location" -> "/board"

    HttpResponse(req)(
      status = MovedPermanently,
      header = Map(cookieHeader, locationHeader),
      body = "")
  }

  def post: Action = req => {
    val posts = getStringFromFile(path2PostData) match {
      case Some(json) => read[List[Post]](json)
      case None => throw new Exception("route file not found")
    }

    val user = req.session.map(_.id).flatMap(id => getUsers find (_.id == id))
    user match {
      case None =>
        emitError(req)(BadRequest)
      case Some(u) =>
        val newPosts = {
          val date = Some(new java.util.Date())
          val content = req.body.getOrElse("content", "")
            .replaceAll( """\r\n|\n""", "<br>")

          posts :+ Post(
            true,
            escape(u.id) getOrElse "",
            date,
            escape(content) getOrElse "")
        }

        writeWithResult(path2PostData)(pw => {
          pw.print(write(newPosts))
          pw.flush()
          signedIn(req)
        })(ex => emitError(req)(InternalServerError))
    }
  }


  //TODO: ajax用に公開
  private def validate4Id(id: String): Boolean = {
    !isExistingUser(id) &&
      (id forall { c =>
        c != '<' || c != '>' || c != '"' || c != '\'' || c != '\\'
      })
  }

  private def validate4Pwd(pwd: String, reinput: String): Boolean = {
    pwd == reinput &&
      (pwd forall { c =>
        c != '<' || c != '>' || c != '"' || c != '\'' || c != '\\'
      })
  }

  //TODO: ajax用に公開
  private def isExistingUser(id: String): Boolean = {
    getUsers exists (_.id == id)
  }

  private def getUsers: List[User] = {
    getStringFromFile(path2UserData) match {
      case Some(json) => read[List[User]](json)
      case None => throw new Exception("route file not found")
    }
  }
}
