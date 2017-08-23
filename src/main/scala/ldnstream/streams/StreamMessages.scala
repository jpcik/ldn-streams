package ldnstream.streams

import akka.http.scaladsl.model._

trait StreamMsg {
  val uri:Uri
  val ct:ContentType.NonBinary
  val body:String
}

object StreamMsg {
  def Msg(b:String)=StreamMsgImpl(null,null,b)
  def Msg(b:String,ct:ContentType.NonBinary)=StreamMsgImpl(null,ct,b)
  def Msg(uri:Uri)=StreamMsgImpl(uri,null,null)
}


trait RequestMsg {//extends StreamMsg{
  val msg:StreamMsg
  val range:MediaRange
}
trait ResponseMsg {//extends StreamMsg{
  val msg:StreamMsg
  val status:StatusCode
}

case class StreamMsgImpl(uri:Uri,ct:ContentType.NonBinary,body:String) extends StreamMsg

import StreamMsg._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.MediaRange

case class OkMsg(msg:StreamMsg) extends ResponseMsg {
  val status=StatusCodes.OK
}
case class ErrorMsg(msg:StreamMsg,status:StatusCode) extends ResponseMsg
  
object EmptyMsg extends StreamMsg{
  val uri=null
  val ct=null
  val body=null
}

case class RetrieveAllStreams(implicit range:MediaRange) 
  extends RequestMsg {
  val msg=EmptyMsg
}
case class RetrieveStream(uri:Uri)(implicit val range:MediaRange)
  extends RequestMsg {
  val msg=Msg(uri)
}
case class CreateStream(msg:StreamMsg)(implicit val range:MediaRange) 
  extends RequestMsg{
}
case class CreateQuery(b:String)(implicit val range:MediaRange)
  extends RequestMsg{
  val msg=Msg(b)
}
case class RetrieveStreamItem(uri:Uri)(implicit val range:MediaRange)
  extends RequestMsg{
  val msg=Msg(uri)
}
case class SendStreamItem(uri:Uri,msg:StreamMsg) extends RequestMsg {
  val range=MediaRanges.`*/*`
}
case class PushStreamItems(uri:Uri)(implicit val range:MediaRange) extends RequestMsg {
  val msg=EmptyMsg
}



