package renesca

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import renesca.graph._
import renesca.parameter.PropertyValue
import renesca.parameter.implicits._

@RunWith(classOf[JUnitRunner])
class QueryHandlerDbSpec extends IntegrationSpecification {

  def createNode(query:String):(Graph,Node) = {
    val graph = db.queryGraph(query)
    val node = graph.nodes.head
    (graph, node)
  }
  
  def resultNode:Node = {
    val resultGraph = db.queryGraph("match n return n")
    resultGraph.nodes.head
  }

  def resultRelation:Relation = {
    val resultGraph = db.queryGraph("match ()-[r]-() return r")
    resultGraph.relations.head
  }

  def testNodeSetProperty(data:PropertyValue) = {
    val (graph,node) = createNode("create n return n")

    node.properties("key") = data
    db.persistChanges(graph)

    resultNode.properties("key") mustEqual data
  }

  def testRelationSetProperty(data:PropertyValue) = {
    val graph = db.queryGraph("create (chicken)-[r:EATS]->(horse) return chicken, r, horse")
    val relation = graph.relations.head

    relation.properties("key") = data
    db.persistChanges(graph)

    val resultRelation = db.queryGraph("match ()-[r]-() return r").relations.head
    resultRelation.properties("key") mustEqual data
  }

  "QueryHandler" should {
    "throw exception on Neo4j Error" in {
      db.query("this is invalid cypher syntax") must throwA[RuntimeException]
    }

    "return only graphs on queryGraphs" in todo
    "return only parameters on queryTables" in todo
    "return no data on query" in todo
  }

  "QueryHandler.persist" should {

    "set long property on node" in { testNodeSetProperty(123) }
    "set double property on node" in { testNodeSetProperty(1.337) }
    "set string property on node" in { testNodeSetProperty("schnipp") }
    "set boolean property on node" in { testNodeSetProperty(true) }

    "set long array property on node" in { testNodeSetProperty(List(1, 3)) }
    "set double array property on node" in { testNodeSetProperty(List(1.7, 2.555555)) }
    "set string array property on node" in { testNodeSetProperty(List("schnipp","schnapp")) }
    "set boolean array property on node" in { testNodeSetProperty(List(true, false)) }

    "remove property from node" in {
      val (graph,node) = createNode("create n set n.yes = 0 return n")

      node.properties -= "yes"
      db.persistChanges(graph)

      resultNode.properties must beEmpty
    }

    "set label on node" in {
      val (graph,node) = createNode("create n return n")

      node.labels += Label("BEER")
      db.persistChanges(graph)

      resultNode.labels must contain(exactly(Label("BEER")))
    }

    "remove label from node" in {
      val (graph,node) = createNode("create (n:WINE) return n")

      node.labels -= Label("WINE")
      db.persistChanges(graph)

      resultNode.labels must beEmpty
    }

    "delete node" in {
      val (graph,node) = createNode("create n return n")

      graph.delete(node)
      db.persistChanges(graph)

      val resultGraph = db.queryGraph("match n return n")
      resultGraph.nodes must beEmpty
    }

    "delete node with relations" in {
      // 1. create (m)-r->(n)<-l-(q)
      // 2. query (m)-r->(n)
      // 3. delete n (implicitly deletes relation r from graph and relation l which is only in the database)
      // 4. whole graph should be (m) and (q)

      val nid = db.queryGraph("create n return n").nodes.head.id
      val graph = db.queryGraph(Query(
        "match n where id(n) = {id} create (m)-[r:INTERNAL]->(n)<-[l:EXTERNAL]-(q) return n,r,m",
        Map("id" -> nid)))

      graph.nodes must haveSize(2) // m, n
      graph.relations must haveSize(1) // r

      val n = graph.nodes.find(_.id == nid).get
      graph.delete(n) // deletes node n and relations l,r
      db.persistChanges(graph)

      val resultGraph = db.queryGraph("match (n) optional match (n)-[r]-() return n,r")
      resultGraph.nodes must haveSize(2)
      resultGraph.nodes must not contain n
      resultGraph.relations must beEmpty
    }

    "set long property on relation" in { testRelationSetProperty(123) }
    "set double property on relation" in { testRelationSetProperty(1.337) }
    "set string property on relation" in { testRelationSetProperty("schnipp") }
    "set boolean property on relation" in { testRelationSetProperty(true) }

    "set long array property on relation" in { testRelationSetProperty(List(1, 3)) }
    "set double array property on relation" in { testRelationSetProperty(List(1.7, 2.555555)) }
    "set string array property on relation" in { testRelationSetProperty(List("schnipp","schnapp")) }
    "set boolean array property on relation" in { testRelationSetProperty(List(true, false)) }

    "remove property from relation" in {
      val graph = db.queryGraph("create (chicken)-[r:EATS]->(horse) set r.yes = 100 return chicken, r, horse")
      val relation = graph.relations.head

      relation.properties -= "yes"
      db.persistChanges(graph)

      resultRelation.properties must beEmpty
    }

    "delete relation" in {
      val graph = db.queryGraph("create (chicken)-[r:EATS]->(horse) return chicken, r, horse")
      val relation = graph.relations.head

      graph.delete(relation)
      db.persistChanges(graph)

      val resultGraph = db.queryGraph("match (n) optional match (n)-[r]-() return n,r")
      resultGraph.nodes must haveSize(2)
      resultGraph.relations must beEmpty
    }

    "add node" in {
      val graph = Graph.empty
      val node = graph.addNode()
      node.id.value must beLessThan(0L)
      db.persistChanges(graph)
      node.id.value must beGreaterThan(0L)

      resultNode.id mustEqual node.id
      resultNode.labels must beEmpty
      resultNode.properties must beEmpty
    }

    "add properties and labels after NodeAdd" in {
      val graph = Graph.empty
      val node = graph.addNode()
      node.properties += ("test" -> 5)
      node.labels ++= Set("foo", "bar")
      db.persistChanges(graph)

      resultNode.properties mustEqual Map("test" -> 5)
      resultNode.labels must contain(exactly(Label("foo"), Label("bar")))
    }

    "set properties and labels in NodeAdd" in {
      val graph = Graph.empty
      graph.addNode(Set("foo", "bar"), Map("test" -> 5))
      db.persistChanges(graph)

      resultNode.properties mustEqual Map("test" -> 5)
      resultNode.labels must contain(exactly(Label("foo"), Label("bar")))
    }

    "add relation" in {
      val graph = Graph.empty
      val start = graph.addNode(Set("I"))
      val end = graph.addNode(Set("cheezburger"))
      val relation = graph.addRelation(start, end, "can haz")
      relation.id.value must beLessThan(0L)
      db.persistChanges(graph)
      relation.id.value must beGreaterThan(0L)

      resultRelation mustEqual Relation(relation.id, start, end, RelationType("can haz"), Map.empty)
    }

    "add properties after RelationAdd" in {
      val graph = Graph.empty
      val start = graph.addNode(Set("I"))
      val end = graph.addNode(Set("cheezburger"))
      val relation = graph.addRelation(start, end, "can haz")
      relation.properties += ("one" -> "yes")
      db.persistChanges(graph)

      resultRelation mustEqual Relation(relation.id, start, end, RelationType("can haz"), Map("one" -> "yes"))
    }

    "set properties in RelationAdd" in {
      val graph = Graph.empty
      val start = graph.addNode(Set("I"))
      val end = graph.addNode(Set("cheezburger"))
      val relation = graph.addRelation(start, end, "can haz", Map("one" -> "yes"))
      db.persistChanges(graph)

      resultRelation mustEqual Relation(relation.id, start, end, RelationType("can haz"), Map("one" -> "yes"))
    }
  }

}

