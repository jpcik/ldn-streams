package ldnstream.core

import scala.collection.mutable.HashMap
import akka.http.scaladsl.model.Uri

class NotificationHandler(base:Uri) {
  val notifications=new HashMap[String,Notification]
  var count=0
  def create(payload:Payload)={
    val id=s"${base}notification_${count.toString}"
    count+=1;
    notifications+= (id->Notification(id,payload))
    println("created: "+payload.getString)
    id
  }
  def retrieve(id:String)=notifications.get(id)
  def retrieveAll=notifications.values
}

trait Payload {
  def getString:String
}

case class JsonLdPayload(jsonld:String) extends Payload{
  def getString=jsonld
}

case class TurtlePayload(turtle:String) extends Payload{
  def getString=turtle
}

case class Notification(id:String, payload:Payload)