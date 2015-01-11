package board.entity

case class Post(enabled: Boolean, id: String, userId: String, date: Option[java.util.Date], content: String)
