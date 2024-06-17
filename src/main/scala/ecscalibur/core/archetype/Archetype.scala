package ecscalibur.core.archetype

import ecscalibur.core.Components.{Component, ComponentId, ComponentType}
import ecscalibur.core.Entities.Entity
import ecscalibur.id.IdGenerator

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.*

private[core] object Archetypes:
  trait Archetype:
    def hasSignature(types: ComponentType*): Boolean
    def handles(types: ComponentType*): Boolean
    def add(e: Entity, comps: Array[Component]): Unit
    def contains(e: Entity): Boolean
    def remove(e: Entity): Unit
    def get(e: Entity, comp: ComponentType): Component

  object Archetype:
    def apply(types: ComponentType*): Archetype =
      require(types.nonEmpty, "Given signature is empty.")
      val distinct = types.distinct
      require(distinct.length == types.length, "Given signature has duplicate component types.")
      ArchetypeImpl(distinct)

    private class ArchetypeImpl(types: Seq[ComponentType]) extends Archetype:
      private val signature: Array[ComponentId] = types.map(~_).toArray.sorted
      private val entityIndexes: mutable.HashMap[Entity, Int] = mutable.HashMap.empty
      private val components: Map[ComponentId, ArrayBuffer[Component]] =
        signature.view.map(t => t -> ArrayBuffer.empty[Component]).to(HashMap)
      private val idGenerator: IdGenerator = IdGenerator()

      override inline def hasSignature(types: ComponentType*): Boolean =
        require(types.nonEmpty, "Given signature to test is empty.")
        hasSignatureInternal(types.map(~_))

      private inline def hasSignatureInternal(ids: Seq[ComponentId]): Boolean =
        signature.sameElements(ids.sorted)

      override inline def handles(types: ComponentType*): Boolean =
        require(types.nonEmpty, "Given type sequence is empty.")
        signature.containsSlice(types.map(~_).sorted)

      override inline def add(e: Entity, comps: Array[Component]): Unit =
        require(!entityIndexes.contains(e), "Attempted to readd an already existing entity.")
        require(
          hasSignatureInternal(comps.map(~_)),
          "Given component types do not correspond to this archetype's signature."
        )
        val newEntityIdx = idGenerator.next
        entityIndexes += e -> newEntityIdx
        for c <- comps do
          if newEntityIdx >= components(~c).length then components(~c) += c
          else components(~c).update(newEntityIdx, c)

      override inline def contains(e: Entity): Boolean = idGenerator.isValid(entityIndexes(e))

      override def remove(e: Entity): Unit =
        inline val errorMsg = "Attempted to remove an entity not stored in this archetype."
        require(entityIndexes.contains(e), errorMsg)
        val idx = entityIndexes(e)
        require(idGenerator.isValid(idx), errorMsg)
        idGenerator.erase(idx)

      override inline def get(e: Entity, comp: ComponentType): Component =
        require(contains(e), "Failed to find the given entity.")
        require(handles(comp), "Given type is not part of this archetype's signature.")
        components(~comp)(entityIndexes(e))

  import ecscalibur.core.Components.Annotations.component
  @component
  private[Archetypes] class NilComponent extends Component
  private[Archetypes] object NilComponent extends ComponentType
