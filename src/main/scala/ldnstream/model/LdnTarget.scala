package ldnstream.model

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.headers.LinkParams._
import akka.http.scaladsl.server.Directives._
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.Lang
import java.io.StringWriter
import rspactors.vocab.LDP
import rdftools.rdf.jena._

trait LdnTarget extends LdnNode {
 import MediaTypes._
 import LdnTypes._ 
  private val inboxLink={
    val inboxRel=List(new rel(LDP.inbox.iri.path))
    LinkValue(Uri("/inbox"),inboxRel)
  }
 
  val rdfTypes = Seq(MediaRange(`text/turtle`))



  def payloadInbox(uri:Uri,lang:Lang)={
    val m=ModelFactory.createDefaultModel
    val thisResource=ResourceFactory.createResource(uri.toString)
    val inboxProp=toJenaProperty(LDP.inbox)
    val inboxUri=ResourceFactory.createResource(s"${uri}inbox")
    m.add(thisResource,inboxProp,inboxUri)
    val sw=new StringWriter
    RDFDataMgr.write(sw, m,lang)
    sw.toString
  }
  
  
  val targetRoute = { 
    pathSingleSlash {
      (get & headerValue(accepts(`text/turtle`)) & extractUri) { (m,uri) =>
         complete{ 
           HttpResponse(StatusCodes.OK,List(headers.Link(inboxLink)),
                  HttpEntity(`text/turtle`,payloadInbox(uri,Lang.TURTLE)))
         }            
      } ~      
      (get & headerValue(accepts(`application/ld+json`)) & extractUri) { (m,uri) =>
         complete{ 
           HttpResponse(StatusCodes.OK,List(headers.Link(inboxLink)),
                  HttpEntity(`application/ld+json`,payloadInbox(uri,Lang.JSONLD)))
         }            
      } ~      
     (get | head) {
       complete(
          HttpResponse(headers=List(headers.Link(inboxLink)))
          //,entity=HttpEntity(ContentTypes.`text/plain(UTF-8)`,"The inbox is here"))
       )
     }
  }}
}
