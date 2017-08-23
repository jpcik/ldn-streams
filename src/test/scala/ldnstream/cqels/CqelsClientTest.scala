package ldnstream.cqels

import ldnstream.streams.LdnStreamClient
import ldnstream.streams.StreamTarget
import akka.http.scaladsl.model.ContentType
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import ldnstream.model.LdnTypes._

object CqelsClientTest {

  def main(args:Array[String])={
    val client=new LdnStreamClient("cqelsClient")
    
    implicit val target=StreamTarget("http://localhost:8080/streams")
    implicit val cType=`application/ld+json`
    client.createReadStream("s1")
    client.getStreams()
    
  }
}