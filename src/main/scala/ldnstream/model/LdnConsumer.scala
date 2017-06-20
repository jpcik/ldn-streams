package ldnstream.model

import ldnstream.Consumer
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.Http

import concurrent.duration._
import java.io.StringReader
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import collection.JavaConversions._
import ldnstream.ReceiverRef
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.RDFNode
import java.io.StringWriter

trait LdnConsumer extends Consumer with LdnNode{
  def getNotificationUris(inbox:Uri,mediaType:MediaType=`application/ld+json`)={
     val resp = Http().singleRequest(HttpRequest(GET,inbox,List(Accept(mediaType))))
    resp.flatMap{r=>r.entity.toStrict(5.second).map { e=>e.data}.map(_.utf8String).map { pay =>
      println(toRdfLang(mediaType))
      val m=ModelFactory.createDefaultModel
      val sr=new StringReader(pay)
      
      RDFDataMgr.read(m, sr, "",toRdfLang(mediaType))
       println("titpi")

      val inboxProp:Property=LdnVocab.contains
      val obj:RDFNode=null
      val trip=m.listStatements(inbox, inboxProp, obj).toSeq
        .map(s=>s.getObject.asResource.toString).toArray
        
        println(trip.mkString(" "))
      trip
    }}
  }
  
  def getNotification(notifUri:Uri,mediaType:MediaType=`application/ld+json`)={
     val resp = Http().singleRequest(HttpRequest(GET,notifUri,List(Accept(mediaType))))
    resp.flatMap{r=>r.entity.toStrict(5.second).map { e=>e.data}.map(_.utf8String).map { pay =>
   //   val m=ModelFactory.createDefaultModel
pay  //    sr.toString
    }}
  }
  
  
}