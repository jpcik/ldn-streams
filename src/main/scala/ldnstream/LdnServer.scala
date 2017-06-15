package ldnstream

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import scala.io.StdIn
import ldnstream.model.LdnTarget
import ldnstream.model.LdnReceiver

object LdnServer extends LdnTarget with LdnReceiver{
  lazy val host:String=boot._1
  lazy val port=boot._2

  def boot={
    val p=sys.env.get("PORT")
    println(s"detected port: $p")
    if (p.isDefined)
      "ldnstreams.herokuapp.com"->p.get.toInt
    else
      "localhost"->8080  
  }
  
  def main(args: Array[String]) {

    implicit val system = ActorSystem("ldn-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
     
    val route = receiverRoute

    
    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", port)
    println(s"Server online at http://localhost:${port}/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    while (true){
      Thread.sleep(10000)
    }
    bindingFuture
      .flatMap{o=>
        println("unbinding")
        o.unbind()} // trigger unbinding from the port
      .onComplete{p =>
        println("terminating server")
        system.terminate()} // and shutdown when done
  }
}





