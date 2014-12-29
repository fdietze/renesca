package renesca.graph

import org.junit.runner.RunWith
import org.specs2.mock._
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import renesca.json
import renesca.json.{LongPropertyValue, Relationship, StringPropertyValue}

import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class GraphFactorySpec extends Specification with Mockito {
  trait ExampleGraph extends Scope {
    // A<--B
    //  \_7
    val A = Node(1)
    val B = Node(2)
    val ArB = Relation(4, A, B)
    val BrA = Relation(5, B, A)

    val nodesList = List(A,B)
    val relationsList = List(ArB, BrA)

    implicit val graph = Graph(nodesList, relationsList)
  }

  "Graph" should {
    "create graphs with nodes and relations" in new ExampleGraph {

      import graph._

      nodes must containTheSameElementsAs(nodesList)
      relations must containTheSameElementsAs(relationsList)
    }
  }

  "Graph from JSON factory" should {
    "create empty graph" in {
      val jsonGraph = json.Graph()

      val graph = Graph(jsonGraph)

      graph.nodes must beEmpty
      graph.relations must beEmpty
    }

    "create graph with single empty node" in {
      val jsonGraph = json.Graph(List(json.Node("1744")))

      val graph = Graph(jsonGraph)
      val node = graph.nodes.head

      node.id mustEqual 1744
      node.labels mustEqual Nil
      node.properties mustEqual Map.empty
    }

    "create graph with single node with labels" in {
      val jsonGraph = json.Graph( List(json.Node("1744", labels = List("hopfen", "malz"))))

      val graph = Graph(jsonGraph)
      val node = graph.nodes.head

      node.labels mustEqual mutable.Set(Label("hopfen"), Label("malz"))
    }

    "create graph with single node with properties" in {
      import renesca.json.PropertyValue._
      val jsonGraph = json.Graph( List(json.Node("1744", labels = Nil, properties = Map("biene" -> "honig", "bier" -> 1516L))))

      val graph = Graph(jsonGraph)
      val node = graph.nodes.head

      node.properties mustEqual mutable.Map("biene" -> StringPropertyValue("honig"), "bier" -> LongPropertyValue(1516))
    }

    "create graph with relations containing nodes" in {
      val jsonGraph = json.Graph(
        nodes = List(json.Node("1744"), json.Node("1516")),
        relationships = List(Relationship("42",startNode = "1744", endNode = "1516", `type` = "wurst"))
      )

      val graph = Graph(jsonGraph)

      graph.nodes must have size 2
      graph.relations must have size 1

      val relation = graph.relations.head
      relation.id mustEqual 42
      relation.relationType mustEqual RelationType("wurst") // !!!
      relation.startNode mustEqual graph.nodes.find(_.id == 1744).get
      relation.endNode mustEqual graph.nodes.find(_.id == 1516).get
    }

    "create graph with relation with properties" in {
      val jsonGraph = json.Graph(
        nodes = List(json.Node("1744"), json.Node("1516")),
        relationships = List(Relationship("42",startNode = "1744", endNode = "1516", `type` = "wurst",
          properties = Map("ki" -> "wäl", "freitag" -> 13L)))
      )

      val graph = Graph(jsonGraph)
      val relation = graph.relations.head

      relation.properties mustEqual mutable.Map("ki" -> StringPropertyValue("wäl"), "freitag" -> LongPropertyValue(13))
    }
  }
}
