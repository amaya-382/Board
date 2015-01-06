package board.entity

case class Post(enabled: Boolean, name: String, date: Option[java.util.Date], content: String, imgs: List[String])
