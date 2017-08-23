package ldnstream

import akka.http.scaladsl.model.Uri
import scala.concurrent.Future



trait Target {
  val uri:Uri
}

case class Receiver(inboxUri:Uri)



//case class Target(uri:String,inbox:Receiver) extends LdnEntity
/*
case class ReceiverRef(inboxUri:Uri) extends Receiver

trait Receiver {
  val inboxUri:Uri
}

trait Sender extends LdnEntity{
  def discover(targetUrl:String):Future[Option[Receiver]]
  def send(receiver:Receiver,payload:Any):Unit
}
*/