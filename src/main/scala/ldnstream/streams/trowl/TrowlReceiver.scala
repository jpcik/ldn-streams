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
import org.semanticweb.owlapi.model.OWLAxiom
import language.implicitConversions
import org.semanticweb.owlapi.model.OWLDataFactory
import rdftools.rdf.Iri
import org.semanticweb.owlapi.model.OWLImportsDeclaration
import org.semanticweb.owlapi.model.OWLAnnotation
import org.semanticweb.owlapi.model.AddAxiom
import org.semanticweb.owlapi.model.AddImport
import org.semanticweb.owlapi.model.AddOntologyAnnotation
import org.semanticweb.owlapi.model.OWLAnnotationProperty
import org.semanticweb.owlapi.model.OWLAnnotationValue
import rdftools.rdf.Literal
import org.semanticweb.owlapi.model.OWLLiteral
import rdftools.rdf.vocab.RDFS
import org.semanticweb.owlapi.model.OWLEntity
import org.semanticweb.owlapi.model.OWLClassExpression
import org.semanticweb.owlapi.model.OWLClass
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom
import org.semanticweb.owlapi.model.OWLRestriction
import org.semanticweb.owlapi.model.OWLObjectProperty
import org.semanticweb.owlapi.model.OWLDatatypeRestriction
import org.semanticweb.owlapi.model.OWLObjectRestriction
import org.semanticweb.owlapi.model.OWLDataRestriction
import org.semanticweb.owlapi.model.OWLDataProperty
import org.semanticweb.owlapi.model.OWLIndividual
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom
import language.postfixOps

object Manchester {
  implicit val om=OWLManager.createOWLOntologyManager()
  implicit val f=om.getOWLDataFactory
  implicit def iri2iri(iri:Iri)=IRI.create(iri.path)
  implicit def lit2Literal(lit:Literal)=f.getOWLLiteral(lit.value.toString)
  
  
  import rdftools.rdf.RdfTools._
  //Class (":Person").
  //  SubClassOf (Coco,Topo,Papo).
  //  EquivalentTo (Tipo, Cuco, Mato)

  trait PropAssertion {
    def :: (pa:PropAssertion)= List(this,pa)
  }
  case class ObjPropAssertion(prop:OWLObjectProperty,ind:OWLIndividual) extends PropAssertion
  case class NegObjPropAssertion(prop:OWLObjectProperty,ind:OWLIndividual) extends PropAssertion
  
  class IndividualFrame(ind:OWLIndividual){
    def Types(types:OWLClassExpression*)={
    }
  }
  
  case class Individual(ind:OWLIndividual)
      (Types:OWLClassExpression* )
      (Facts:PropAssertion*) 
      (SameAs:Seq[OWLIndividual] ){
  
  }
  object Individual {
    def apply(iri:Iri,Types:Seq[OWLClassExpression],
      Facts:Seq[PropAssertion],
      SameAs:Seq[OWLIndividual]=Seq.empty):Individual=Individual(f.getOWLNamedIndividual(iri))(Types:_*)(Facts:_*)(SameAs)
     
      //def apply(iri:Iri)(Types:OWLClassExpression*):Individual=Individual(f.getOWLNamedIndividual(iri))(Types:_*)()()
  }
  
  case class ObjectProperty(op:OWLObjectProperty)
      (Domain:OWLClassExpression*)
      (Range:OWLClassExpression*)
  
  class ClassMan(iri:Iri,subclassOf:Set[OWLSubClassOfAxiom],
      equivalentTo:Set[OWLEquivalentClassesAxiom]){
    def SubClassOf(classexps:OWLClassExpression*)={
      val newsubs=subclassOf ++ classexps.map{cl=>
        f.getOWLSubClassOfAxiom(f.getOWLClass(iri), cl)
      }
      new ClassMan(iri,newsubs,equivalentTo)
    }
    def EquivalentTo(classexps:OWLClassExpression*)={
      val newsubs=equivalentTo ++ classexps.map{cl=>
        f.getOWLEquivalentClassesAxiom(f.getOWLClass(iri), cl)
      }
      new ClassMan(iri,subclassOf,newsubs)
    }
    
      
  }

  
  // owl:Thing that hasFirstName exactly 1 and hasFirstName only string[minLength 1]
 
  // thing.that(hasFirstName.exactly(1))
  
  implicit class ObjProp(op:OWLObjectProperty){
    def some(cls:OWLClassExpression)={
      f.getOWLObjectSomeValuesFrom(op, cls)
      
    }
    def exactly(i:Int)={
      f.getOWLObjectExactCardinality(i, op)
    }
    
    def only(cls:OWLClassExpression)={
      f.getOWLObjectAllValuesFrom(op, cls)
    }
    
    def apply(ind:OWLIndividual)=ObjPropAssertion(op,ind)
  }
  
  object not {
    def apply(restr:OWLClassExpression)={
      f.getOWLObjectComplementOf(restr)
    }
    def apply(opAss:ObjPropAssertion)={
      NegObjPropAssertion(opAss.prop,opAss.ind)
    }
  }
  
  implicit class DataProp(d:OWLDataProperty){
    def only(dt:OWLDatatypeRestriction)={
      f.getOWLDataAllValuesFrom(d, dt)
    }
    
  }
  
  implicit class DataRestr(r:OWLDatatypeRestriction){
    def and (rest:OWLDatatypeRestriction)={
      f.getOWLDataIntersectionOf(r,rest)
    }
  }

  implicit class Restr(r:OWLObjectRestriction){
    def and (rest:OWLObjectRestriction)={
      f.getOWLObjectIntersectionOf(r,rest)
    }
  }
  
  
  implicit class ClsXpr(r:OWLClassExpression) {
    def and(rest:OWLClassExpression)={
      f.getOWLObjectIntersectionOf(r,rest)
    }
    def :: (cl:OWLClassExpression)=List(r,cl)

  }
  
  implicit class ClassExp(c:OWLClass){
    def that(ax:OWLObjectRestriction)= 
      f.getOWLObjectIntersectionOf(c,ax)
  }

  implicit class Indi(i:OWLIndividual) {
    def :: (i2:OWLIndividual)=  List (i,i2)
  }
  
  
  val hasFirstName=f.getOWLObjectProperty(Iri("hasFirstName"))
  
  val nn=not (hasFirstName exactly 1)
  
  clazz("moto") that 
        (hasFirstName only clazz("") ) and 
        {hasFirstName some clazz("") } and 
        not (hasFirstName exactly 1) 
  
  hasFirstName only clazz("")   and (hasFirstName only clazz("") ) 
  val ind=f.getOWLNamedIndividual(Iri("indi"))
    
  val indi=Individual ("iri",
      Types= clazz("loro") ::  (hasFirstName exactly 1) :: clazz("") ,
      Facts=Seq())                                                                                                                                                                                                                                                                                                                                                                                                                                                               
  
  Individual( "coso",
     Types=  clazz("mono") ::
             (hasFirstName exactly 1) ,
     Facts=  (hasFirstName (ind)) :: 
             (not (hasFirstName (ind)) ) ,
     SameAs= ind :: ind )
  
  ObjectProperty(hasFirstName) (
      Domain=clazz(""),clazz("") ) (
      Range= hasFirstName exactly 1)
      
  object Class {
    def apply(iri:Iri)=new ClassMan(iri,Set.empty,Set.empty)
  }
  
    def clazz(iri:Iri)=f.getOWLClass(iri)

  
  def cosas ={
   import rdftools.rdf.RdfTools._

    val pip=
      Class("mono").
        SubClassOf(clazz("dibi") that (hasFirstName exactly 1)).
        EquivalentTo(clazz("toto") and (hasFirstName some clazz("")))
  }
  
}

object OwlApi {
  implicit val om=OWLManager.createOWLOntologyManager()
  implicit val f=om.getOWLDataFactory
  implicit def iri2iri(iri:Iri)=IRI.create(iri.path)
  implicit def lit2Literal(lit:Literal)=f.getOWLLiteral(lit.value.toString)
  def createOntology(iri:Iri)=om.createOntology(iri)
  def individual(iri:Iri)=f.getOWLNamedIndividual(iri)
  def clazz(iri:Iri)=f.getOWLClass(iri)
  def objectProperty(iri:Iri)=f.getOWLObjectProperty(iri)
  def dataPoperty(iri:Iri)=f.getOWLDataProperty(iri)
  
  def declaration(entity:OWLEntity)=f.getOWLDeclarationAxiom(entity)
  
  def prefix(prefixName:String,fullIri:String)(implicit f:OWLDataFactory)={
     val imports=   f.getOWLImportsDeclaration(Iri(""))
     
  }
  def imports(iri:Iri)=f.getOWLImportsDeclaration(iri)
  
  def subClassOf(subClass:OWLClassExpression,superClass:OWLClassExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLSubClassOfAxiom(subClass, superClass,annotations.asJava)
  def equivalentClasses(class1:OWLClassExpression,class2:OWLClassExpression,annotations:Set[OWLAnnotation])=
    f.getOWLEquivalentClassesAxiom(class1, class2,annotations.asJava)
  def equivalentClasses(classes:Set[OWLClassExpression],annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLEquivalentClassesAxiom(classes.asJava,annotations.asJava)
  def disjointClasses(classes:Set[OWLClassExpression],annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLDisjointClassesAxiom(classes.asJava, annotations.asJava)
  def disjointUnion(cls:OWLClass,classes:Set[OWLClassExpression],annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLDisjointUnionAxiom(cls,classes.asJava, annotations.asJava)
    
  def annotationProperty(iri:Iri)=f.getOWLAnnotationProperty(iri)
  def annotation(annProperty:OWLAnnotationProperty,annValue:OWLAnnotationValue)=
    f.getOWLAnnotation(annProperty, annValue)
  def annotation(iriProp:Iri,irival:Iri):OWLAnnotation=
    annotation(annotationProperty(iriProp), irival:IRI)
  def annotation(iriProp:Iri,litval:Literal):OWLAnnotation=
    annotation(annotationProperty(iriProp), litval:OWLLiteral)
  def ontology(iri:Iri)(imports:OWLImportsDeclaration*)(annotations:OWLAnnotation*)(axioms:OWLAxiom*)={
    val o=createOntology(iri)
    imports.foreach{imp=>
      om.applyChange(new AddImport(o, imp))
    }
    annotations.foreach{annot=>
      om.applyChange(new AddOntologyAnnotation(o,annot))
    }
    axioms.foreach {axiom=>
      om.applyChange(new AddAxiom(o,axiom))  
    }
    o
  }
}

class TrowlReceiver(iri:String) extends ActorStreamReceiver{
  import OwlApi._
  import rdftools.rdf.RdfTools._
  import rdftools.rdf.RdfSchema._
  //val selects=new collection.mutable.HashMap[String,CsparqlQueryResultProxy]

  //val csparql=new CsparqlEngineImpl
  //csparql.initialize()
  
  
  //implicit val fact=om.getOWLDataFactory
  
  val onto=
    ontology("http://example.org/") {
      imports("http://importedOnto1.org/")
      imports("http://importedOnto2.org/") }  {
        
      annotation(RDFS.label,lit("helo")) }  {
        
      declaration(clazz("Clase"))
      declaration(objectProperty("someProp"))
      subClassOf(clazz("Copo"), clazz("Jipo"))
      equivalentClasses(Set(clazz("Toto"),clazz("Mimi"))) }
  
  
  
  val relfactory = new RELReasonerFactory();
  val reasoner = relfactory.createReasoner(onto);
  val ind=individual("coco")
  val cls=clazz("clase")
//  val axiom=fact.getOWLClassAssertionAxiom(cls,ind)
  
//  val toAdd:Set[OWLAxiom]=Set(axiom)
//  reasoner.add(toAdd.asJava)
  val serverIri=Uri(iri)
    
  override def consumeGraph(uri:Uri,g:Graph)={
  
    g.triples.foreach { t =>
      println("feed cqels "+uri)
      //csparql.getStreamByIri(uri.toString).put(
       //   new RdfQuadruple(t.s.toString,t.p.toString,t.o.toString,System.currentTimeMillis))  
    }
  }
  
  //"SELECT ?s ?p ?o WHERE {STREAM <e.com/stream> [RANGE 2s] {?s ?p ?o}}"
  override def query(name:String,queryStr:String,
      insert:Map[String,String]=>Unit)={
    
    //val slct=csparql.registerQuery(queryStr, false)
    //slct.addObserver(setUpListener(insert))
    //selects.put(name,slct)
  }
 
  override def push(id:String,insert:Map[String,String]=>Unit)={
    //val con=setUpListener(insert)
    //selects(id).addObserver(con)
    ResultHandler("")
  }
  
  override def terminatePush(id:String,hnd:ResultHandler)={
    //selects(id).deleteObserver(hnd.native.asInstanceOf[Observer])
  }
  
}