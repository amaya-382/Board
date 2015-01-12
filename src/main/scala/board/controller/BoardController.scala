package board.controller

import board.entity._

import org.json4s._
import org.json4s.native.Serialization.{read, write}
import com.tristanhunt.knockoff.DefaultDiscounter._

import simplehttpserver.impl._
import simplehttpserver.util.{Security, EasyEmit}
import simplehttpserver.util.implicits.Implicit._
import simplehttpserver.util.Security._
import simplehttpserver.util.Common._

import scala.util.Random


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


  def signUpPage: Action = req => {
    val contentType = "Content-Type" -> html.contentType
    val title = "Sing up"
    val head = """<script src="/js/jquery-1.11.2.min.js" type="text/javascript"></script>
                 |<script src="/js/board_signup.js" type="text/javascript"></script>
                 |<link href='http://fonts.googleapis.com/css?family=Rock+Salt' rel='stylesheet' type='text/css'>
                 |<link href='http://fonts.googleapis.com/css?family=Oswald:700' rel='stylesheet' type='text/css'>
                 |<link href='http://fonts.googleapis.com/css?family=Open+Sans+Condensed:300' rel='stylesheet' type='text/css'>
                 |<link href="/css/board.css" rel="stylesheet" type="text/css">
                 |<link href="/css/board_sign.css" rel="stylesheet" type="text/css">
                 | """.stripMargin

    HttpResponse(req)(
      status = Ok,
      header = Map(contentType),
      body = buildWithBase(Seq(
        "title" -> title,
        "head" -> head,
        "body" -> (signUpBase getOrElse "")
      )))
  }

  def signInPage: Action = req => {
    val contentType = "Content-Type" -> html.contentType
    val title = "Sign in"
    val head = """<script src="/js/jquery-1.11.2.min.js" type="text/javascript"></script>
                 |<link href='http://fonts.googleapis.com/css?family=Rock+Salt' rel='stylesheet' type='text/css'>
                 |<link href='http://fonts.googleapis.com/css?family=Oswald:700' rel='stylesheet' type='text/css'>
                 |<link href='http://fonts.googleapis.com/css?family=Open+Sans+Condensed:300' rel='stylesheet' type='text/css'>
                 |<link href="/css/board.css" rel="stylesheet" type="text/css">
                 |<link href="/css/board_sign.css" rel="stylesheet" type="text/css">
                 | """.stripMargin

    HttpResponse(req)(
      status = Ok,
      header = Map(contentType),
      body = buildWithBase(Seq(
        "title" -> title,
        "head" -> head,
        "body" -> (signInBase getOrElse "")
      )))
  }

  def signedIn: Action = req => {
    val contentType = "Content-Type" -> html.contentType
    val title = "Board"
    val head = """<script src="/js/jquery-1.11.2.min.js" type="text/javascript"></script>
                 |<script src="/js/jquery.pjax.min.js" type="text/javascript"></script>
                 |<script src="/js/board_timeLine.js" type="text/javascript"></script>
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
        val isOwn = user.exists(_.id == post.userId)
        buildWithPostBase(Seq(
          "isOwn" -> (if (isOwn) "own" else ""),
          "display" -> (if (isOwn) "" else "none"),
          "id" -> post.id,
          "name" -> userList.find(_.id == post.userId).map(_.name).getOrElse("x"),
          "date" -> post.date.map(_.formatted("%tF %<tT")).mkString,
          "content" -> toXHTML(knockoff(
            post.content.replaceAll( """\\r\\n""", "\r\n"))).toString
        ))
      })

    val cookieHeader = {
      val sessionId = req.session.map(_.sessionId).getOrElse("")
      "Set-Cookie" -> s"SESSIONID=$sessionId; path=/board; HttpOnly"
    }

    val timeLine = buildWithTimeLineBase(Seq(
      "posts" -> formed.mkString,
      "token" -> (req.session.map(_.sessionId).map(hashBySHA384) getOrElse "")
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
      if (!isValidId(id))
        signUpPage(req) //TODO:msg表示
      else if (!isValidPwd(pwd, repwd))
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
      case _ if !req.body.get("token").exists(req.session.map(_.sessionId).map(hashBySHA384).contains) =>
        emitError(req)(BadRequest)
      case None =>
        emitError(req)(BadRequest)
      case Some(u) =>
        val date = new java.util.Date()
        val postId = hashBySHA384(u.id + date + Random.alphanumeric.take(5).mkString)
        val content = req.body.getOrElse("content", "")
          .replaceAll( """\r\n|\n""", """\\r\\n""")
        val newPosts = {
          val dateOpt = Some(date)

          posts :+ Post(
            true,
            postId,
            u.id,
            dateOpt,
            escape(content) getOrElse "")
        }

        writeWithResult(path2PostData)(pw => {
          pw.print(write(newPosts))
          pw.flush()

          if (req.header.get("X-Requested-With").contains("XMLHttpRequest"))
            HttpResponse(req)(
              status = Ok,
              header = Map(),
              body = buildWithPostBase(Seq(
                "isOwn" -> "own",
                "display" -> "",
                "id" -> postId,
                "name" -> u.name,
                "date" -> date.formatted("%tF %<tT").mkString,
                "content" -> toXHTML(knockoff(
                  content.replaceAll( """\\r\\n""", "\r\n"))).toString)))
          else
            signedIn(req)
        })(ex => emitError(req)(InternalServerError))
    }
  }

  def delete: Action = req => {
    val posts = getStringFromFile(path2PostData) match {
      case Some(json) => read[List[Post]](json)
      case None => throw new Exception("route file not found")
    }

    val user = req.session.map(_.id).flatMap(id => getUsers find (_.id == id))
    user match {
      case _ if !req.body.get("token").exists(req.session.map(_.sessionId).map(hashBySHA384).contains) =>
        emitError(req)(BadRequest)
      case None =>
        emitError(req)(BadRequest)
      case Some(u) =>
        req.body.get("id") match {
          case Some(id) =>
            var deleted = false
            val newPosts = posts.map(post =>
              if (id == post.id && u.id == post.userId) {
                deleted = true
                post.copy(enabled = false)
              }
              else
                post
            )

            if (deleted)
              writeWithResult(path2PostData)(pw => {
                pw.print(write(newPosts))
                pw.flush()
                HttpResponse(req)(
                  status = Ok,
                  header = Map("Content-Type" -> txt.contentType),
                  body = id
                )
              })(ex => emitError(req)(InternalServerError))
            else
              emitError(req)(BadRequest)
          case None =>
            emitError(req)(InternalServerError)
        }
    }
  }

  def ajax_isValidId: Action = req => {
    val contentType = "Content-Type" -> js.contentType

    HttpResponse(req)(
      status = Ok,
      header = Map(contentType),
      body = if (isValidId(req.req._2.replaceFirst( """.+?\?""", ""))) "1" else ""
    )
  }

  private def isValidId(id: String): Boolean = {
    id != "" && !isExistingUser(id)
  }

  private def isValidPwd(pwd: String, reinput: String): Boolean = {
    pwd == reinput
  }

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
