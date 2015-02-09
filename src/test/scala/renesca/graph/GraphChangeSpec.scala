package renesca.graph

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import renesca.parameter.StringPropertyValue
import renesca.parameter.implicits._

@RunWith(classOf[JUnitRunner])
class GraphChangeSpec extends Specification with Mockito {

  "Graph" should {
    "collect all changes in one collection and clear it" in {
      val graphChange = mock[GraphChange]

      val nodesChange = mock[GraphChange]
      val relationsChange = mock[GraphChange]

      val nodeLabelChange = mock[GraphChange]
      val nodePropertiesChange = mock[GraphChange]

      val relationPropertiesChange = mock[GraphChange]

      val A = Node(1)
      A.labels.localChanges += nodeLabelChange
      A.properties.localChanges += nodePropertiesChange

      val B = Node(2)

      val ArB = Relation(3, A, B, "r")
      ArB.properties.localChanges += relationPropertiesChange

      val graph = Graph(List(A,B), List(ArB))
      graph.localChanges += graphChange
      graph.nodes.localChanges += nodesChange
      graph.relations.localChanges += relationsChange

      A.changes must contain(exactly(nodeLabelChange, nodePropertiesChange))
      ArB.changes must contain(exactly(relationPropertiesChange))

      graph.changes.size mustEqual 6
      graph.changes must contain(exactly(
        graphChange,
        nodesChange,
        relationsChange,
        nodeLabelChange,
        nodePropertiesChange,
        relationPropertiesChange
      ))

      graph.clearChanges()

      graph.changes must beEmpty
    }

    "emit change when adding node to nodes" in {
      val graph = Graph.empty

      graph.nodes += Node()

      val nodeId = graph.nodes.head.id
      graph.nodes.localChanges must contain(exactly(
        NodeAdd(nodeId).asInstanceOf[GraphChange]
      ))
    }

    "emit change when deleting node from nodes" in {
      val A = Node(1)
      val graph = Graph(List(A), Nil)

      graph.nodes -= A

      graph.nodes.localChanges must contain(exactly(
        NodeDelete(A.id).asInstanceOf[GraphChange]
      ))
    }

    "emit changes when deleting node with relations from nodes" in {
      val A = Node(1)
      val B = Node(2)
      val C = Node(4)
      val ArB = Relation(3, A, B, "r")
      val BrC = Relation(5, B, C, "r")
      val graph = Graph(List(A, B, C), List(ArB, BrC))

      graph.nodes -= A

      graph.nodes must not contain A
      graph.relations must not contain ArB
      graph.relations must contain (BrC)
      graph.changes must not contain RelationDelete(3)
      graph.changes must not contain RelationDelete(5)
    }

    "emit change when deleting relation" in {
      val A = Node(1)
      val B = Node(2)
      val ArB = Relation(3, A, B, "r")
      val graph = Graph(List(A,B), List(ArB))

      graph.relations -= ArB

      graph.changes must contain(exactly(
        RelationDelete(ArB.id).asInstanceOf[GraphChange]
      ))
    }

    "emit change when adding local node with properties/labels" in {
      val graph = Graph.empty
      val node = Node()

      node.properties("ciao") = "mit V"
      node.labels += "boom"
      graph.nodes += node

      graph.changes must contain(allOf(
        NodeAdd(node.id).asInstanceOf[GraphChange],
        NodeSetProperty(node.id, "ciao", "mit V").asInstanceOf[GraphChange],
        NodeSetLabel(node.id, "boom").asInstanceOf[GraphChange]
      )).inOrder
    }

    "emit change when adding local node and then properties/labels" in {
      val graph = Graph.empty
      val node = Node()

      graph.nodes += node
      node.properties("ciao") = "mit V"
      node.labels += "boom"

      graph.changes must contain(allOf(
        NodeAdd(node.id).asInstanceOf[GraphChange],
        NodeSetProperty(node.id, "ciao", "mit V").asInstanceOf[GraphChange],
        NodeSetLabel(node.id, "boom").asInstanceOf[GraphChange]
      )).inOrder
    }

    "emit change when adding local relation with properties" in {
      val node1 = Node(1)
      val node2 = Node(2)
      val graph = Graph(List(node1, node2))
      val relation = Relation(start = node1, end = node2, relationType = "nagut")

      relation.properties("ciao") = "mit V"
      graph.relations += relation

      graph.changes must contain(allOf(
        RelationAdd(relation.id, relation.startNode.id, relation.endNode.id, relation.relationType).asInstanceOf[GraphChange],
        RelationSetProperty(relation.id, "ciao", "mit V").asInstanceOf[GraphChange]
      )).inOrder
    }

    "emit change when adding local relation and then properties" in {
      val node1 = Node(1)
      val node2 = Node(2)
      val graph = Graph(List(node1, node2))
      val relation = Relation(start = node1, end = node2, relationType = "nagut")

      graph.relations += relation
      relation.properties("ciao") = "mit V"

      graph.changes must contain(allOf(
        RelationAdd(relation.id, relation.startNode.id, relation.endNode.id, relation.relationType).asInstanceOf[GraphChange],
        RelationSetProperty(relation.id, "ciao", "mit V").asInstanceOf[GraphChange]
      )).inOrder
    }
  }

  "Node" should {
    "emit change when setting property" in {
      val properties = new Properties(1, NodeSetProperty , NodeRemoveProperty)

      properties("key") = "value"
      properties += ("key" -> "value")

      properties.localChanges must contain(exactly(
        NodeSetProperty(1, "key", "value").asInstanceOf[GraphChange],
        NodeSetProperty(1, "key", "value").asInstanceOf[GraphChange]
      ))
    }

    "emit change when removing property" in {
      val properties = new Properties(1, NodeSetProperty , NodeRemoveProperty)

      properties -= "key"

      properties.localChanges must contain(exactly(
          NodeRemoveProperty(1, "key").asInstanceOf[GraphChange]
      ))
    }

    "emit change when setting label" in  {
      val labels = new NodeLabels(1)
      val label = mock[Label]

      labels += label

      labels.localChanges must contain(exactly(
        NodeSetLabel(1, label).asInstanceOf[GraphChange]
      ))
    }

    "emit change when removing label" in  {
      val labels = new NodeLabels(1)
      val label = mock[Label]

      labels -= label

      labels.localChanges must contain(exactly(
        NodeRemoveLabel(1, label).asInstanceOf[GraphChange]
      ))
    }
  }

  "Relation" should {
    "emit change when setting property" in {
      val relation = Relation(1, Node(2), Node(3), "r")
      relation.properties += ("key" -> "value")
      relation.properties -= "key"

      relation.changes must contain(
        RelationSetProperty(1, "key", "value")
      )
    }

    "emit change when removing property" in {
      val relation = Relation(1, Node(2), Node(3), "r")
      relation.properties += ("key" -> "value")
      relation.properties -= "key"

      relation.changes must contain(
          RelationSetProperty(1, "key", StringPropertyValue("value")).asInstanceOf[GraphChange],
          RelationRemoveProperty(1, "key").asInstanceOf[GraphChange]
      ).inOrder
    }
  }
}
