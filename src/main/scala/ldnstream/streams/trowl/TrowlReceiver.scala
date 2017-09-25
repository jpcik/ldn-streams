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
import org.semanticweb.owlapi.model.OWLObjectPropertyCharacteristicAxiom
import org.semanticweb.owlapi.model.OWLObject
import org.semanticweb.owlapi.model.OWLProperty
import org.semanticweb.owlapi.model.OWLPropertyExpression
import org.semanticweb.owlapi.model.OWLAnnotationSubject
import org.semanticweb.owlapi.model.OWLDataRange
import org.semanticweb.owlapi.model.OWLDatatype
import org.semanticweb.owlapi.model.OWLDataPropertyExpression
import org.semanticweb.owlapi.vocab.OWLFacet
import org.semanticweb.owlapi.model.OWLFacetRestriction
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper
import org.semanticweb.owlapi.model.OWLOntology
import rdftools.rdf.Class
import org.semanticweb.owlapi.model.PrefixManager
import org.semanticweb.owlapi.util.DefaultPrefixManager
import rdftools.rdf.Bnode

object Manchester {
  implicit val om=OWLManager.createOWLOntologyManager()
  implicit val f=om.getOWLDataFactory
  implicit def iri2iri(iri:Iri)=IRI.create(iri.path)
  implicit def lit2Literal(lit:Literal)=lit.value match {
    case i:Int=>f.getOWLLiteral(i)
    case d:Double=>f.getOWLLiteral(d)
    case s:String=>f.getOWLLiteral(s)
    case b:Boolean=>f.getOWLLiteral(b)
  }
  
  
  import rdftools.rdf.RdfTools._
  //Class (":Person").
  //  SubClassOf (Coco,Topo,Papo).
  //  EquivalentTo (Tipo, Cuco, Mato)

  trait PropAssertion {
    def :: (pa:PropAssertion)= List(this,pa)
  }
  case class ObjPropAssertion(prop:OWLObjectProperty,ind:OWLIndividual) extends PropAssertion
  case class NegObjPropAssertion(prop:OWLObjectProperty,ind:OWLIndividual) extends PropAssertion
  
  trait PropertyCharacteristic
  
  object Functional extends PropertyCharacteristic
  object InverseFunctional extends PropertyCharacteristic
  object Reflexive extends PropertyCharacteristic
  object Irreflexive extends PropertyCharacteristic
  object Symmetric extends PropertyCharacteristic
  object Asymmetric extends PropertyCharacteristic
  object Transitive extends PropertyCharacteristic
  
  class ObjectPropertyFrame(op:OWLObjectProperty)
    (Characteristics:Seq[PropertyCharacteristic])
    (Domain:Seq[OWLClassExpression])
    (Range:Seq[OWLClassExpression]) {
    val domainAxioms=Domain map {domain=>
     
      f.getOWLObjectPropertyDomainAxiom(op, domain)
    }
    val characteristicAxioms:Seq[OWLObjectPropertyCharacteristicAxiom]=Characteristics map {
      case Functional=>f.getOWLFunctionalObjectPropertyAxiom(op)
      case InverseFunctional=>f.getOWLInverseFunctionalObjectPropertyAxiom(op)
      case Reflexive=>f.getOWLReflexiveObjectPropertyAxiom(op)
      case Irreflexive=>f.getOWLIrreflexiveObjectPropertyAxiom(op)
      case Symmetric=>f.getOWLSymmetricObjectPropertyAxiom(op)
      case Asymmetric=>f.getOWLAsymmetricObjectPropertyAxiom(op)
      case Transitive=>f.getOWLTransitiveObjectPropertyAxiom(op)     
    }
    if (Characteristics.contains(Functional))
      f.getOWLFunctionalObjectPropertyAxiom(op)
  }
  
  class Annotated(o:OWLClassExpression,a:Set[OWLAnnotation]){
    def :: (ann:Annotated)=List(this,ann)
    
  }
  
  
  implicit class OWLObjectPlus[T<:OWLClassExpression](o:T) extends Annotated(o,Set.empty){
    def annot(as:OWLAnnotation*)=new Annotated(o,as.toSet)    
    def annotations=annot _
    
  }
  
  
  
  class IndividualFrame(ind:OWLIndividual){
    def Types(types:OWLClassExpression*)={
    }
  }
  
  case class Individual(ind:OWLIndividual)
      (Types:Annotated* )
      (Facts:PropAssertion*) 
      (SameAs:Seq[OWLIndividual] ){
  
  }
  object Individual {
    def apply(iri:Iri,Types:Seq[Annotated],
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
    //def :: (cl:OWLClassExpression)=List(r,cl)

  }
  
  implicit class ClassExp(c:OWLClass){
    def that(ax:OWLObjectRestriction)= 
      f.getOWLObjectIntersectionOf(c,ax)
    def ⊑ (ce:OWLClassExpression)=
      f.getOWLSubClassOfAxiom(c, ce)
  }

  implicit class Indi(i:OWLIndividual) {
    def :: (i2:OWLIndividual)=  List (i,i2)
  }
  
  
  val hasFirstName=f.getOWLObjectProperty(Iri("hasFirstName"))
  
  val nn=not (hasFirstName exactly 1)


  clazz("topo") ⊑ clazz("pipo")
 
  clazz("moto") that 
        (hasFirstName only clazz("") ) and 
        {hasFirstName some clazz("") } and 
        not (hasFirstName exactly 1) 
  
  hasFirstName only clazz("")   and (hasFirstName only clazz("") ) 
  val ind=f.getOWLNamedIndividual(Iri("indi"))
    
  val indi=Individual ("iri",
      Types= clazz("loro")  ::  (hasFirstName exactly 1 annot null) :: (clazz("") annot null) ,
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
  type Annotations=Set[OWLAnnotation]
  val øA = Set.empty[OWLAnnotation]
  
  implicit val om=OWLManager.createOWLOntologyManager()
  implicit val f=om.getOWLDataFactory
  implicit def iri2iri(iri:Iri)=IRI.create(iri.path)
  implicit def bnode2Anon(bnode:Bnode)=f.getOWLAnonymousIndividual(bnode.id)
  implicit def lit2Literal(lit:Literal)=lit.value match {
    case i:Int=>f.getOWLLiteral(i)
    case d:Double=>f.getOWLLiteral(d)
    case s:String=>f.getOWLLiteral(s)
    case b:Boolean=>f.getOWLLiteral(b)
  }
  implicit def clazz2OwlCLass(c:Class)=f.getOWLClass(c.iri)
  implicit def iri2Range(iri:Iri)=f.getOWLDatatype(iri)
  
  def createOntology(iri:Iri)=om.createOntology(iri)
  
  implicit class OwlStringContext(sc:StringContext)(implicit prefixes:PrefixManager) {
    def i(args:Any*)=OwlApi.individual(sc.parts.mkString)
    def ind(args:Any*)=i(args) 
    def c(args:Any*)=OwlApi.Class(sc.parts.mkString)
    def op(args:Any*)=OwlApi.objectProperty(sc.parts.mkString)
    def dp(args:Any*)=OwlApi.dataProperty(sc.parts.mkString)
    def ap(args:Any*)=OwlApi.annotationProperty(sc.parts.mkString)

  }
  
  def individual(iri:Iri)=f.getOWLNamedIndividual(iri)
  def individual(name:String)(implicit prefixes:PrefixManager)={
    f.getOWLNamedIndividual(name, prefixes)
  }
  def clazz(iri:Iri)=f.getOWLClass(iri)
  def Class(shortIri:String)(implicit prefixes:PrefixManager)={
    f.getOWLClass(shortIri, prefixes)  
  }
  def Datatype(shortIri:String)(implicit prefixes:PrefixManager)={
    f.getOWLDatatype(shortIri, prefixes)
  }
  def NamedIndividual(shortIri:String)(implicit prefixes:PrefixManager)=
    f.getOWLNamedIndividual(shortIri,prefixes)
  
  def objectProperty(iri:Iri)=f.getOWLObjectProperty(iri)
  def objectProperty(name:String)(implicit prefixes:PrefixManager)=
    f.getOWLObjectProperty(name, prefixes)
  def dataProperty(iri:Iri)=f.getOWLDataProperty(iri)
  def dataProperty(name:String)(implicit prefixes:PrefixManager)={
    f.getOWLDataProperty(name, prefixes)
  }
  def annotationProperty(iri:Iri)=f.getOWLAnnotationProperty(iri)
  def annotationProperty(name:String)(implicit prefixes:PrefixManager)=
    f.getOWLAnnotationProperty(name, prefixes)
  
  def declaration(entity:OWLEntity)=f.getOWLDeclarationAxiom(entity)
  
  def imports(iri:Iri)=f.getOWLImportsDeclaration(iri)
  
  // classes
  def subClassOf(subClass:OWLClassExpression,superClass:OWLClassExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLSubClassOfAxiom(subClass, superClass,annotations.asJava)
  def equivalentClasses(class1:OWLClassExpression,class2:OWLClassExpression,annotations:Set[OWLAnnotation])=
    f.getOWLEquivalentClassesAxiom(class1, class2,annotations.asJava)
  def equivalentClasses(classes:Set[OWLClassExpression],annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLEquivalentClassesAxiom(classes.asJava,annotations.asJava)
  def equivalentClasses(classes:OWLClassExpression*)=
    f.getOWLEquivalentClassesAxiom(classes.toSet.asJava)
  def disjointClasses(classes:Set[OWLClassExpression],annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLDisjointClassesAxiom(classes.asJava, annotations.asJava)
  def disjointClasses(classes:OWLClassExpression*)=
    f.getOWLDisjointClassesAxiom(classes:_*)
  def disjointUnion(cls:OWLClass,classes:Set[OWLClassExpression],annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLDisjointUnionAxiom(cls,classes.asJava, annotations.asJava)
  def disjointUnion(cls:OWLClass,classes:OWLClassExpression*)=
    f.getOWLDisjointUnionAxiom(cls,classes.toSet.asJava)
  def hasKey(cls:OWLClassExpression,props:Set[OWLPropertyExpression],annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLHasKeyAxiom(cls, props.asJava,annotations.asJava)
    
  //annotations
  def annotationAssertion(prop:OWLAnnotationProperty,subj:OWLAnnotationSubject,
      value:OWLAnnotationValue, annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLAnnotationAssertionAxiom(prop, subj, value,annotations.asJava)
  def annotationAssertion(subj:OWLAnnotationSubject,
      annotation:OWLAnnotation, annotations:Set[OWLAnnotation])=
    f.getOWLAnnotationAssertionAxiom(subj,annotation,annotations.asJava)
  def annotationAssertion(subj:OWLAnnotationSubject,
      annotation:OWLAnnotation)=
    f.getOWLAnnotationAssertionAxiom(subj,annotation)
     
  //datatypes
  def datatypeDefinition(dtype:OWLDatatype,range:OWLDataRange,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLDatatypeDefinitionAxiom(dtype,range,annotations.asJava)
  
  //object properties
  def objectPropertyDomain(prop:OWLObjectPropertyExpression,cls:OWLClassExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLObjectPropertyDomainAxiom(prop, cls,annotations.asJava)
  def objectPropertyRange(prop:OWLObjectPropertyExpression,cls:OWLClassExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLObjectPropertyRangeAxiom(prop, cls,annotations.asJava)
  def objectFunctionalProperty(prop:OWLObjectPropertyExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLFunctionalObjectPropertyAxiom(prop,annotations.asJava)
  def objectInverseFunctionalProperty(prop:OWLObjectPropertyExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLInverseFunctionalObjectPropertyAxiom(prop,annotations.asJava)  
  def objectReflexiveProperty(prop:OWLObjectPropertyExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLReflexiveObjectPropertyAxiom(prop,annotations.asJava)
  def objectIrreflexiveProperty(prop:OWLObjectPropertyExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLIrreflexiveObjectPropertyAxiom(prop,annotations.asJava)
  def objectSymmetricProperty(prop:OWLObjectPropertyExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLSymmetricObjectPropertyAxiom(prop,annotations.asJava)
  def objectAsymmetricProperty(prop:OWLObjectPropertyExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLAsymmetricObjectPropertyAxiom(prop,annotations.asJava)
  def objectTransitiveProperty(prop:OWLObjectPropertyExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLTransitiveObjectPropertyAxiom(prop,annotations.asJava)
  def subObjectPropertyOf(subProp:OWLObjectPropertyExpression,superProp:OWLObjectPropertyExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLSubObjectPropertyOfAxiom(subProp, superProp,annotations.asJava)
  def equivalentObjectProperties(props:Set[OWLObjectPropertyExpression],annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLEquivalentObjectPropertiesAxiom(props.asJava,annotations.asJava)
  def equivalentObjectProperties(prop1:OWLObjectPropertyExpression,prop2:OWLObjectPropertyExpression,annotations:Set[OWLAnnotation])=
    f.getOWLEquivalentObjectPropertiesAxiom(prop1,prop2,annotations.asJava)  
  def equivalentObjectProperties(prop1:OWLObjectPropertyExpression,prop2:OWLObjectPropertyExpression)=
    f.getOWLEquivalentObjectPropertiesAxiom(prop1,prop2)  
  def disjointObjectProperties(props:Set[OWLObjectPropertyExpression],annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLDisjointObjectPropertiesAxiom(props.asJava,annotations.asJava)
  def inverseObjectProperties(prop1:OWLObjectPropertyExpression,prop2:OWLObjectPropertyExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLInverseObjectPropertiesAxiom(prop1,prop2,annotations.asJava)

  //data properties
  def dataPropertyDomain(prop:OWLDataPropertyExpression,cls:OWLClassExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLDataPropertyDomainAxiom(prop, cls,annotations.asJava)
  def dataPropertyRange(prop:OWLDataPropertyExpression,range:OWLDataRange,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLDataPropertyRangeAxiom(prop, range,annotations.asJava)
  def functionalDataProperty(prop:OWLDataPropertyExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLFunctionalDataPropertyAxiom(prop,annotations.asJava)
  def subDataPropertyOf(subProp:OWLDataPropertyExpression, superProp:OWLDataPropertyExpression,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLSubDataPropertyOfAxiom(subProp,superProp,annotations.asJava)
  def equivalentDataProperties(prop1:OWLDataPropertyExpression,prop2:OWLDataPropertyExpression,annotations:Set[OWLAnnotation])=
    f.getOWLEquivalentDataPropertiesAxiom(prop1,prop2,annotations.asJava)
  def equivalentDataProperties(prop1:OWLDataPropertyExpression,prop2:OWLDataPropertyExpression)=
    f.getOWLEquivalentDataPropertiesAxiom(prop1,prop2)
  def equivalentDataProperties(props:Set[OWLDataPropertyExpression],annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLEquivalentDataPropertiesAxiom(props.asJava,annotations.asJava)
  def disjointDataProperties(props:Set[OWLDataPropertyExpression],annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLDisjointDataPropertiesAxiom(props.asJava,annotations.asJava)
  
  // annotation properties
  def annotationPropertyDomain(prop:OWLAnnotationProperty,iri:Iri,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLAnnotationPropertyDomainAxiom(prop, iri,annotations.asJava)
  def annotationPropertyRange(prop:OWLAnnotationProperty,iri:Iri,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLAnnotationPropertyRangeAxiom(prop, iri,annotations.asJava)
  def subAnnotationPropertyOf(subProp:OWLAnnotationProperty,superProp:OWLAnnotationProperty,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLSubAnnotationPropertyOfAxiom(subProp, superProp, annotations.asJava)
    
  // individuals
  def classAssertion(cls:OWLClassExpression,ind:OWLIndividual,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLClassAssertionAxiom(cls, ind,annotations.asJava)
  def objectPropertyAssertion(prop:OWLObjectPropertyExpression,subj:OWLIndividual,obj:OWLIndividual,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLObjectPropertyAssertionAxiom(prop, subj, obj,annotations.asJava)
  def negativeObjectPropertyAssertion(prop:OWLObjectPropertyExpression,subj:OWLIndividual,obj:OWLIndividual,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLNegativeObjectPropertyAssertionAxiom(prop, subj, obj,annotations.asJava)
  def dataPropertyAssertion(prop:OWLDataPropertyExpression,subj:OWLIndividual,obj:OWLLiteral,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLDataPropertyAssertionAxiom(prop, subj, obj,annotations.asJava)
  def negativeDataPropertyAssertion(prop:OWLDataPropertyExpression,subj:OWLIndividual,obj:OWLLiteral,annotations:Set[OWLAnnotation]=Set.empty)=
    f.getOWLNegativeDataPropertyAssertionAxiom(prop, subj, obj,annotations.asJava)
  def sameIndividual(inds:Set[OWLIndividual],annotations:Annotations=øA)=
    f.getOWLSameIndividualAxiom(inds.asJava,annotations.asJava)
  def sameIndividual(inds:OWLIndividual*)=
    f.getOWLSameIndividualAxiom(inds.toSet.asJava)
  def differentIndividuals(inds:Set[OWLIndividual],annotations:Annotations=øA)=
    f.getOWLDifferentIndividualsAxiom(inds.asJava,annotations.asJava)
  def differentIndividuals(inds:OWLIndividual*)=
    f.getOWLDifferentIndividualsAxiom(inds.toSet.asJava)
    
  // datatype restrictions
  def datatypeRestriction(dtype:OWLDatatype,facet:OWLFacet,lit:OWLLiteral)=
    f.getOWLDatatypeRestriction(dtype, facet, lit)
  def datatypeRestriction(dtype:OWLDatatype,facets:Set[OWLFacetRestriction])=
    f.getOWLDatatypeRestriction(dtype, facets.asJava)
  def dataOneOf(literals:Set[OWLLiteral])=
    f.getOWLDataOneOf(literals.asJava)
  def dataOneOf(literals:OWLLiteral*)=
    f.getOWLDataOneOf(literals:_*)
  def dataComplementOf(range:OWLDataRange)=
    f.getOWLDataComplementOf(range)
  def dataIntersectionOf(ranges:Set[OWLDataRange])=
    f.getOWLDataIntersectionOf(ranges.asJava)
  def dataIntersectionOf(ranges:OWLDataRange*)=
    f.getOWLDataIntersectionOf(ranges:_*)
  def dataUnionOf(ranges:Set[OWLDataRange])=
    f.getOWLDataUnionOf(ranges.asJava)
  def dataUnionOf(ranges:OWLDataRange*)=
    f.getOWLDataUnionOf(ranges:_*)
    
  // object restrictions
  def inverseObjectProperty(prop1:OWLObjectPropertyExpression,prop2:OWLObjectPropertyExpression,annotations:Annotations=øA)=
    f.getOWLInverseObjectPropertiesAxiom(prop1, prop2,annotations.asJava)
  def objectOneOf(inds:Set[OWLIndividual])=
    f.getOWLObjectOneOf(inds.asJava)
  def objectOneOf(inds:OWLIndividual*)=
    f.getOWLObjectOneOf(inds.toSet.asJava)
  def objectSomeValuesFrom(prop:OWLObjectPropertyExpression,cls:OWLClassExpression)=
    f.getOWLObjectSomeValuesFrom(prop, cls)
  def objectAllValuesFrom(prop:OWLObjectPropertyExpression,cls:OWLClassExpression)=
    f.getOWLObjectAllValuesFrom(prop, cls)
  def objectHasValue(prop:OWLObjectPropertyExpression,ind:OWLIndividual)=
    f.getOWLObjectHasValue(prop, ind)
  def objectMinCardinality(i:Int,prop:OWLObjectPropertyExpression)=
    f.getOWLObjectMinCardinality(i, prop)
  def objectMinCardinality(i:Int,prop:OWLObjectPropertyExpression,cls:OWLClassExpression)=
    f.getOWLObjectMinCardinality(i, prop,cls)
  def objectMaxCardinality(i:Int,prop:OWLObjectPropertyExpression)=
    f.getOWLObjectMaxCardinality(i, prop)
  def objectMaxCardinality(i:Int,prop:OWLObjectPropertyExpression,cls:OWLClassExpression)=
    f.getOWLObjectMaxCardinality(i, prop,cls)
  def objectExactCardinality(i:Int,prop:OWLObjectPropertyExpression)=
    f.getOWLObjectExactCardinality(i, prop)
  def objectExactCardinality(i:Int,prop:OWLObjectPropertyExpression,cls:OWLClassExpression)=
    f.getOWLObjectExactCardinality(i, prop,cls)
  def objectHasSelf(prop:OWLObjectPropertyExpression)=
    f.getOWLObjectHasSelf(prop)
    
  def objectInverseOf(prop:OWLObjectPropertyExpression)=
    f.getOWLObjectInverseOf(prop)
    
  def dataSomeValuesFrom(prop:OWLDataPropertyExpression,range:OWLDataRange)=
    f.getOWLDataSomeValuesFrom(prop, range)
  def dataAllValuesFrom(prop:OWLDataPropertyExpression,range:OWLDataRange)=
    f.getOWLDataAllValuesFrom(prop, range)
  def dataHasValue(prop:OWLDataPropertyExpression,value:OWLLiteral)=
    f.getOWLDataHasValue(prop, value)
  def dataMinCardinality(i:Int,prop:OWLDataPropertyExpression,range:OWLDataRange)=
    f.getOWLDataMinCardinality(i, prop,range)
  def dataMinCardinality(i:Int,prop:OWLDataPropertyExpression)=
    f.getOWLDataMinCardinality(i, prop)
  def dataMaxCardinality(i:Int,prop:OWLDataPropertyExpression,range:OWLDataRange)=
    f.getOWLDataMaxCardinality(i, prop,range)
  def dataMaxCardinality(i:Int,prop:OWLDataPropertyExpression)=
    f.getOWLDataMaxCardinality(i, prop)  
  def dataExactCardinality(i:Int,prop:OWLDataPropertyExpression,range:OWLDataRange)=
    f.getOWLDataExactCardinality(i, prop,range)
  def dataExactCardinality(i:Int,prop:OWLDataPropertyExpression)=
    f.getOWLDataExactCardinality(i, prop)  
    
  def objectComplementOf(cls:OWLClassExpression)=
    f.getOWLObjectComplementOf(cls)
  def objectIntersectionOf(cls:Set[OWLClassExpression])=
    f.getOWLObjectIntersectionOf(cls.asJava)
  def objectIntersectionOf(cls:OWLClassExpression*)=
    f.getOWLObjectIntersectionOf(cls:_*)  
  def objectUnionOf(cls:Set[OWLClassExpression])=
    f.getOWLObjectUnionOf(cls.asJava)
  def objectUnionOf(cls:OWLClassExpression*)=
    f.getOWLObjectUnionOf(cls:_*)
  
  
    
  def annotation(annProperty:OWLAnnotationProperty,annValue:OWLAnnotationValue)=
    f.getOWLAnnotation(annProperty, annValue)
  def annotation(iriProp:Iri,irival:Iri):OWLAnnotation=
    annotation(annotationProperty(iriProp), irival:IRI)
  def annotation(iriProp:Iri,litval:Literal):OWLAnnotation=
    annotation(annotationProperty(iriProp), litval:OWLLiteral)
  def ontology(iri:Iri,imports:Seq[OWLImportsDeclaration]=Seq.empty,annotations:Seq[OWLAnnotation]=Seq.empty,axioms:Seq[OWLAxiom]=Seq.empty)={
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
  //def ontology(iri:Iri,imports:Seq[OWLImportsDeclaration]=Seq.empty):OWLOntology=
  //  ontology(iri,imports,Seq.empty,Seq.empty)
  def ontology(iri:Iri,axioms:Seq[OWLAxiom]):OWLOntology={
    ontology(iri,Seq.empty,Seq.empty,axioms)
  }

  
  implicit class ImportPlus(imports:OWLImportsDeclaration) {
  //  def :: (imp2:OWLImportsDeclaration)=List(imports,imp2)
  }
  
  implicit def ImportsToList(imports:OWLImportsDeclaration)=List(imports)
  implicit def AnnotationToList(annot:OWLAnnotation)=
    List(annot)
 
  implicit def AxiomToList(axiom:OWLAxiom)=List(axiom)  
  
  def Prefix(pref:String,iri:String)(implicit prefixes:PrefixManager)=
    prefixes.setPrefix(pref, iri)
    
  def createPrefixManager=new DefaultPrefixManager
  
  implicit class OntologyPlus(o:OWLOntology) {
    def individuals=o.getIndividualsInSignature()
    def individuals(cls:OWLClassExpression)={
      o.getClassAssertionAxioms(cls).asScala.map {ax=>ax.getIndividual}
    }
    def classExpressions(ind:OWLIndividual)=
      o.getClassAssertionAxioms(ind).asScala.map(_.getClassExpression)
    
    def objectProperties(ind:OWLIndividual)=
      o.getObjectPropertyAssertionAxioms(ind).asScala.map(_.getProperty)
      
    def objectPropertyObjects(ind:OWLIndividual)=
      o.getObjectPropertyAssertionAxioms(ind).asScala.map(t=>(t.getProperty,t.getObject))
    
    
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
  implicit val pref=new DefaultPrefixManager
  val onto=
    ontology("http://example.org/",
      imports("http://importedOnto1.org/") ::
      imports("http://importedOnto2.org/"),
        
      annotation(RDFS.label,lit("helo")) ,  
        
      declaration(clazz("Clase")) ::
      declaration(objectProperty("someProp")) ::
      subClassOf(clazz("Copo"), clazz("Jipo")) ::
      equivalentClasses(clazz("Toto"),clazz("Mimi")) :: Nil )
  
      
  
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