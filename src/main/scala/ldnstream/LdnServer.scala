package ldnstream

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn
import akka.stream.scaladsl.Source
import scala.util.Random
import akka.util.ByteString
import akka.http.scaladsl.model.headers.LinkValue
import akka.http.scaladsl.model.headers.LinkParams.rel
import akka.http.scaladsl.model.headers.LinkParam
import akka.http.scaladsl.server.MediaTypeNegotiator
import akka.http.scaladsl.model.MediaType.WithFixedCharset
import akka.http.scaladsl.server.RequestContext
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.headers.Link
import HttpMethods._
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.rdf.model.ModelFactory
import java.io.StringReader
import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.Lang
import org.apache.jena.rdf.model.ResourceFactory
import scala.collection.JavaConversions._
import ldnstream.model.LdnNode
import ldnstream.model.LdnVocab
import ldnstream.model.LdnTarget
import ldnstream.model.LdnReceiver


object LdnServer extends LdnTarget with LdnReceiver{
  val host:String="localhost"
   val port:Int=8080

  def main(args: Array[String]) {

    implicit val system = ActorSystem("ldn-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher
  // streams are re-usable so we can define it here
    // and use it for every request
 
     
    val route = receiverRoute
    val p=sys.env("PORT")
    println(s"detected port: $p")
    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", p.toInt)

    println(s"Server online at http://localhost:${p}/\nPress RETURN to stop...")
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





