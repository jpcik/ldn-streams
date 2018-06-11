package rspactors.cqels

import scala.io.StdIn

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import rspactors.LdnStreamReceiver

object CqelsLdnReceiver extends LdnStreamReceiver with CqelsReceiver{
  val id=1
  implicit val system = ActorSystem("LdnCqels")
  implicit val materializer= ActorMaterializer()
  val host="localhost"
  val port=8080 

  def main(args:Array[String]):Unit={
    val bindingFuture = Http().bindAndHandle(receiverRoute, host, port)
    //testStream
    //sendSomeData
    //query
    println(s"Server online at $base\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_ ⇒ system.terminate())
  }
}