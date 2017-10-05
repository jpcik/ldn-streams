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

class TrowlReceiver(iri:String) extends ActorStreamReceiver{
  import rdftools.rdf.RdfTools._
  //val selects=new collection.mutable.HashMap[String,CsparqlQueryResultProxy]

  //val csparql=new CsparqlEngineImpl
  //csparql.initialize()
  
  
  //implicit val fact=om.getOWLDataFactory
  implicit val pref=new DefaultPrefixManager
  val onto=
    Ontology("http://example.org/",
      Imports("http://importedOnto1.org/") ::
      Imports("http://importedOnto2.org/"),
        
      Annotation(RDFS.label,lit("helo")) ,  
        
      Declaration(clazz("Clase")) ::
      Declaration(objectProperty("someProp")) ::
      SubClassOf(clazz("Copo"), clazz("Jipo")) ::
      EquivalentClasses(clazz("Toto"),clazz("Mimi")) :: Nil )
  
      
  
  val relfactory = new RELReasonerFactory();
  val reasoner = relfactory.createReasoner(onto);

  val serverIri=Uri(iri)
    
  override def consumeGraph(uri:Uri,g:Graph)={
  
    g.triples.foreach { t =>
      println("feed cqels "+uri)
      
      val toAdd:OWLAxiom=t.o match {
        case lo:Literal=>DataPropertyAssertion(dataProperty(t.p), individual(t.s.asIri), lo)
        case lo:Iri=>ObjectPropertyAssertion(objectProperty(t.p), individual(t.s.asIri), individual(lo))
      }
      
      reasoner.add(Set(toAdd).asJava)
    }
  }
  
  override def query(name:String,queryStr:String,
      insert:Map[String,String]=>Unit)={
    
    val parser = new ManchesterOWLSyntaxClassExpressionParser(f,null)
    val exp=parser.parse("hasFather some Person")
    
    context.system.scheduler.schedule(0 seconds, 20 seconds){
      val inds=reasoner.getIndividuals(exp)
      inds.getFlattened.asScala.map{ind=>
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