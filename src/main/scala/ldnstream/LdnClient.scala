package ldnstream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import ldnstream.model.LdnConsumer
import ldnstream.model.LdnSender

object LdnClient extends LdnSender with LdnConsumer {
  implicit val system = ActorSystem("ldn-client")
  implicit val materializer = ActorMaterializer()
  //implicit val ctx = system.dispatcher
  
  
}