package ldnstream

import akka.http.scaladsl.model.Uri
import scala.concurrent.Future


trait LdnEntity {
  //val uri:String
}

//case class Target(uri:String,inbox:Receiver) extends LdnEntity

case class ReceiverRef(inboxUri:Uri) extends Receiver

trait Receiver {
  val inboxUri:Uri
}

trait Sender extends LdnEntity{
  def discover(targetUrl:String):Future[Option[Receiver]]
  def send(receiver:Receiver,payload:Any):Unit
}

trait Consumer extends LdnEntity{
  
}