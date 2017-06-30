package hdt4s

import org.rdfhdt.hdt.hdt.HDTManager
import org.rdfhdt.hdt.triples.IteratorTripleString
import collection.JavaConversions._
import org.rdfhdt.hdtjena.HDTGraph
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.query.QueryExecutionFactory

object Play {
  def main (args:Array[String])={
  
    def loadSwdf={
      
     
    val hdt=HDTManager.mapHDT("/home/jpc/data/swdf/swdf.hdt", null)
    
    val sr=hdt.search("","http://purl.org/dc/elements/1.1/creator","http://data.semanticweb.org/person/jean-paul-calbimonte")
    sr.foreach { x => 
      
      println(x) }
    
    hdt.search("","http://purl.org/dc/elements/1.1/creator","http://data.semanticweb.org/person/oscar-corcho")
    .foreach { x => 
      
      println(x) }
    
    val g= new HDTGraph(hdt)
    
    val m=ModelFactory.createModelForGraph(g)
    val creator=ResourceFactory.createProperty("http://purl.org/dc/elements/1.1/creator")
    val corcho=ResourceFactory.createResource("http://data.semanticweb.org/person/oscar-corcho")
    m.listStatements(null, creator,corcho).foreach { println }
    }
    val hdt=HDTManager.mapHDT("/home/jpc/data/geonames/geonames.hdt", null)
    
    //val sr=hdt.search("http://sws.geonames.org/3970142/","","")
    //sr.foreach { x => 
      
     // println(x) }
   
    val m=ModelFactory.createModelForGraph(new HDTGraph(hdt))
    val q="""
      SELECT ?a ?name WHERE {
        ?a  <http://www.geonames.org/ontology#countryCode> "BO" .
        ?a ?name "Bolivia" .
      }
      """
    val qef=QueryExecutionFactory.create(q,m)
    qef.execSelect().foreach { x =>
      println(x.get("a"))
      
       }
  }
}