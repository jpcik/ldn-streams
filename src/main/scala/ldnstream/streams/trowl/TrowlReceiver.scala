package ldnstream.streams.trowl

import akka.http.scaladsl.model.Uri
import rdftools.rdf.Graph
import ldnstream.streams.ActorStreamReceiver
import eu.trowl.owlapi3.rel.tms.reasoner.dl.RELReasonerFactory
import org.semanticweb.owlapi.model.OWLOntologyFactory
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import collection.JavaConverters._
import language.implicitConversions
import rdftools.rdf._
import rdftools.rdf.vocab.RDFS
import language.postfixOps
import org.semanticweb.owlapi.model.OWLOntology
import rdftools.rdf.Class
import rdftools.owl.owlapi.OwlApiTools._
import rdftools.owl.owlapi.Functional._
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntax
import org.obolibrary.`macro`.ManchesterSyntaxTool
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser
import org.semanticweb.owlapi.util.DefaultPrefixManager
import org.semanticweb.owlapi.model.OWLAxiom
import concurrent.duration._
import collection.JavaConverters._
import rdftools.rdf.vocab.RDF


class TrowlReceiver(iri:String,id:Int) extends ActorStreamReceiver{
  import rdftools.rdf.RdfTools._
  //val selects=new collection.mutable.HashMap[String,CsparqlQueryResultProxy]

  //val csparql=new CsparqlEngineImpl
  //csparql.initialize()
  
  
  //implicit val fact=om.getOWLDataFactory
  implicit val pref=new DefaultPrefixManager
  Prefix("sosa:","http://www.w3.org/ns/sosa/")
  Prefix("qudt:","http://qudt.org/1.1/schema/qudt#")
  Prefix("ex:","http://example.org/vocab#")
  val onto=
    Ontology("http://example.org/",
      Imports("http://www.w3.org/ns/sosa/") ::
      Imports("http://qudt.org/1.1/schema/qudt#"),
        
      Annotation(RDFS.label,lit("helo")) ,  
        
      Declaration(clazz("Clase")) ::
      Declaration(objectProperty("someProp")) ::
      SubClassOf(
          ObjectIntersectionOf(c"sosa:Observation", 
              ObjectSomeValuesFrom(op"sosa:observedProperty",c"qudt:TemperatureQuantity")),c"ex:TemperatureObservation") ::
      ClassAssertion(c"qudt:TemperatureQuantity", ind"ex:Temperature") ::
      ClassAssertion(c"sosa:Observation",ind"ex:obs1") ::
      EquivalentClasses(clazz("Toto"),clazz("Mimi")) :: Nil )
  
      
  
  val relfactory = new RELReasonerFactory();
  val reasoner = relfactory.createReasoner(onto);

  val serverIri=Uri(iri)
    
  //override def declareStream(uri:String)={}
  override def consumeGraph(uri:Uri,g:Graph)={
  
    g.triples.foreach { t =>
      println("feed cqels "+uri)
      
     
      
      val subjectInd=t.subject match {
        case iri:Iri=>individual(iri)
        case bn:Bnode=>f.getOWLAnonymousIndividual(bn.id) 
      }
      val toAdd:OWLAxiom=
      if (t.predicate == RDF.`type`.iri){
        println("classy: "+t.o)
        ClassAssertion(clazz(t.o.asIri), subjectInd)
      }
        
      else
      t.o match {
        case lo:Literal=>DataPropertyAssertion(dataProperty(t.p), subjectInd, lo)
        case lo:Iri=>ObjectPropertyAssertion(objectProperty(t.p), subjectInd, individual(lo))
        case bn:Bnode=>ObjectPropertyAssertion(objectProperty(t.p),subjectInd, f.getOWLAnonymousIndividual(bn.id))
      }
      reasoner.add(Set(toAdd).asJava)
      reasoner.reclassify
    }
  }
  
  override def query(name:String,queryStr:String,
      insert:Map[String,String]=>Unit)={
    
    //val parser = new ManchesterOWLSyntaxClassExpressionParser(f,null)
    //val exp=parser.parse("ex:TemperatureObservation")
    
    context.system.scheduler.schedule(0 seconds, 2 seconds){
      //reasoner.reclassify
      val inds=reasoner.getInstances(c"ex:TemperatureObservation",true)
      println("only got "+inds.asScala.size)
      
      inds.getFlattened.asScala.map{ind=>
        println("some data "+ind)
        insert(Map("ind"->ind.getIRI.toString))
      }
      
    }
    
    //val slct=csparql.registerQuery(queryStr, false)
    //slct.addObserver(setUpListener(insert))
    //selects.put(name,slct)
  }

  // No push for Trowl
  override def push(id:String,insert:Map[String,String]=>Unit)={
    ResultHandler("")
  }
  
  //No push for Trowl
  override def terminatePush(id:String,hnd:ResultHandler)={
    
  }
  
}