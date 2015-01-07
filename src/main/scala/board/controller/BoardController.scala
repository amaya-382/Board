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
  private val loginBase = getStringFromResources("loginBase.html")
  //  private val buildWithLoginBase = builder.buildHtml(loginBase getOrElse "") _
  private val postBase = getStringFromResources("postBase.html")
  private val buildWithPostBase = builder.buildHtml(postBase getOrElse "") _
  private val boardBase = getStringFromResources("boardBase.html")
  private val buildWithBoardBase = builder.buildHtml(boardBase getOrElse "") _


  def signUpPage: Action = req => {
    val contentType = "Content-Type" -> html.contentType
    val title = "Sing up"

    HttpResponse(req)(
      status = Ok,
      header = Map(contentType),
      body = buildWithBase(Seq(
        "title" -> title,
        "head" -> "",
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
        "head" -> "",
        "body" -> (loginBase getOrElse "")
      )))
  }

  def signedIn: Action = req => {
    val contentType = "Content-Type" -> html.contentType
    val title = "Board"
    val head = """<script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
                 |<script src="/js/script.js" type="text/javascript"></script>
                 |<link href="/css/style.css" rel="stylesheet" type="text/css">""".stripMargin

    val posts = getStringFromFile(path2PostData) match {
      case Some(json) => read[List[Post]](json)
      case None => throw new Exception("route file not found")
    }

    val formed =
      posts
        .withFilter(_.enabled)
        .map(post => {
        buildWithPostBase(Seq(
          "name" -> post.name,
          "date" -> post.date.map(_.formatted("%tF %<tT")).mkString,
          "content" -> post.content
        ))
      })

    val cookieHeader = {
      val sessionId = req.session.map(_.sessionId).getOrElse("")
      "Set-Cookie" -> s"SESSIONID=$sessionId; path=/board;"
    }

    val user = req.session flatMap {
      s => getUsers find (_.id == s.id)
    }
    val body = buildWithBoardBase(Seq(
      "name" -> user.map(_.name).getOrElse(""),
      "posts" -> formed.mkString
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
      name <- req.body.get("name")
    } yield {
      //id, pwd に使えない文字が入っていた場合は再度signUpPageへ
      if (!validate4Id(id) || !validate4Pwd(pwd))
        signUpPage(req) //TODO:msg表示
      else if (isExistingUser(id))
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

          posts :+ Post(
            true,
            escape(u.name) getOrElse "",
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
    id forall { c =>
      c != '<' || c != '>' || c != '"' || c != '\'' || c != '\\'
    }
  }

  private def validate4Pwd(pwd: String): Boolean = {
    pwd forall { c =>
      c != '<' || c != '>' || c != '"' || c != '\'' || c != '\\'
    }
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
