package ldnstream.model

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.headers.LinkParams._
import akka.http.scaladsl.server.Directives._
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.RDFDataMgr
import ldnstream.core.NotificationHandler
import ldnstream.core.JsonLdPayload
import ldnstream.core.TurtlePayload
import org.apache.jena.riot.Lang
import java.io.StringWriter
import org.apache.jena.vocabulary.RDF

trait LdnReceiver extends LdnNode{
  import MediaTypes._
  import HttpMethods._
  val receiverRoute = { 
    path("inbox") {
      (post & extractRequestEntity & extractUri & entity(as[String])) { (req,uri,pay) =>
         complete{ 
           if (matchWithoutParams(req.contentType,`application/ld+json`)){
             println("over here")
             createNotification(pay, req.contentType.mediaType) map {iri=>
               HttpResponse(StatusCodes.Created,List(headers.Location(iri)))
             }
           }
           else HttpResponse(StatusCodes.UnsupportedMediaType)
         }
      } ~ 
      (get) {
        complete{
           val sw=new StringWriter
           RDFDataMgr.write(sw, model, Lang.JSONLD)
          HttpResponse(StatusCodes.OK,entity=HttpEntity(`application/ld+json`,sw.toString))
        }
      } ~
      (options) {
        complete {
          
          HttpResponse(StatusCodes.OK,List(headers.Allow(GET,OPTIONS,POST),RawHeader("Accept-Post",`application/ld+json`.toString)))
        }
      }
    } ~
    path("inbox" / Remaining) {id=>
      (get & extractUri) {uri=>
        complete{
          val notif=getNotification(uri)
          if (notif.isDefined) HttpResponse(StatusCodes.OK,
              entity=HttpEntity(`application/ld+json`,notif.get.payload.getString))
          else   HttpResponse(StatusCodes.NotFound)
        }
      }
    }
  }
  lazy val handler=new NotificationHandler(base+"inbox/")
  
  val model=ModelFactory.createDefaultModel
  lazy val inboxRes=ResourceFactory.createResource(base+"inbox")
  model.add(inboxRes,RDF.`type`,ResourceFactory.createResource(LdnVocab.Container))
  
  def createNotification(payload:String,mType:MediaType)={
    val bodyOp=Some(JsonLdPayload(payload))
   /*   
      mType match {
      case `application/ld+json` => Some(JsonLdPayload(payload))
      case `text/turtle` => Some(TurtlePayload(payload))
      case _ => None
    }*/
    bodyOp.map {body=>
    val id=handler.create(body)
    model.add(inboxRes,ResourceFactory.createProperty(LdnVocab.contains),ResourceFactory.createResource(id))
    Uri(id)
    }
  }
  def getNotification(id:Uri)=handler.retrieve(id.toString)
  
}