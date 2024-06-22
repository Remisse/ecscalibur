package ecscalibur.core.archetype

import ecscalibur.core.Components.{Component, ComponentId, ComponentType}
import ecscalibur.core.Entities.Entity
import ecscalibur.id.IdGenerator

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.*
import scala.reflect.ClassTag
import scala.annotation.targetName
import ecscalibur.core.Components.CSeqs.CSeq

private[core] object Archetypes:
  trait Archetype:
    def signature: Signature
    def handles(types: ComponentType*): Boolean
    def add(e: Entity, entityComponents: CSeq): Unit
    def contains(e: Entity): Boolean
    def remove(e: Entity): CSeq
    def softRemove(e: Entity): Unit
    def readAll(predicate: ComponentId => Boolean, f: (Entity, CSeq) => Unit): Unit
    def writeAll(predicate: ComponentId => Boolean, f: (Entity, CSeq) => CSeq): Unit

  object Archetype:
    @targetName("fromTypes")
    def apply(types: ComponentType*): Archetype = apply(Signature(types*))
    @targetName("fromSignature")
    def apply(signature: Signature): Archetype = ArchetypeImpl(signature)

    private class ArchetypeImpl(inSignature: Signature) extends Archetype:
      private val _signature: Signature = inSignature
      private val entityIndexes: ArrayBuffer[Entity] = ArrayBuffer.empty
      private val components: Map[ComponentId, ArrayBuffer[Component]] =
        _signature.underlying.map(t => t -> ArrayBuffer.empty[Component]).to(HashMap)
      private val idGenerator: IdGenerator = IdGenerator()

      override inline def signature: Signature = _signature

      override def handles(types: ComponentType*): Boolean =
        require(types.nonEmpty, "Given type sequence is empty.")
        Signature(types*) isPartOf _signature

      override def add(e: Entity, entityComponents: CSeq): Unit =
        require(!contains(e), "Attempted to readd an already existing entity.")
        require(
          _signature sameAs Signature(entityComponents.toTypes),
          "Given component types do not correspond to this archetype's signature."
        )
        val newEntityIdx: Int = assignIndexToEntity(e)
        for
          c <- entityComponents.underlying
          compArray = components(c.typeId)
        do
          if newEntityIdx >= compArray.length then compArray += c
          else compArray.update(newEntityIdx, c)

      private inline def assignIndexToEntity(e: Entity): Int =
        val newEntityIdx = idGenerator.next
        if (newEntityIdx >= entityIndexes.length) then entityIndexes += e
        else entityIndexes.update(newEntityIdx, e)
        newEntityIdx

      override inline def contains(e: Entity): Boolean = entityIndexes.contains(e) && idGenerator.isValid(entityIndexes(e))

      override def remove(e: Entity): CSeq =
        inline val errorMsg = "Attempted to remove an entity not stored in this archetype."
        require(contains(e), errorMsg)
        val idx = entityIndexes(e)
        idGenerator.erase(idx) match
          case false => throw new IllegalArgumentException(errorMsg)
          case _     => CSeq(components.map((_, comps) => comps(idx)))

      override def softRemove(e: Entity): Unit =
        inline val errorMsg = "Attempted to remove an entity not stored in this archetype."
        require(contains(e), errorMsg)
        val idx = entityIndexes(e)
        val _ = idGenerator.erase(idx)

      override def readAll(predicate: ComponentId => Boolean, f: (Entity, CSeq) => Unit) =
        val filteredComps = components.filter((id, _) => predicate(id))
        for e <- entityIndexes if contains(e) do
          val inputComps = CSeq(filteredComps.map((_, comps) => comps(e)))
          f(e, inputComps)

      override def writeAll(predicate: ComponentId => Boolean, f: (Entity, CSeq) => CSeq) =
        val filteredComps = components.filter((id, _) => predicate(id))
        val inputIds = filteredComps.map((id, _) => id).toArray
        for e <- entityIndexes if contains(e) do
          val inputComps = CSeq(filteredComps.map((_, comps) => comps(e)))
          val editedComponents: CSeq = f(e, inputComps)
          val returnedSignature = editedComponents.underlying.toSignature
          require(
            returnedSignature sameAs inputIds.toSignature,
            s"Unexpected components returned.\nExpected: ${inputIds.mkString}\nFound: ${returnedSignature.underlying.mkString}"
          )
          for c <- editedComponents.underlying do components(c.typeId).update(entityIndexes(e), c)

      override def equals(x: Any): Boolean = x match
        case a: Archetype => _signature sameAs a.signature
        case _            => false

      override def hashCode(): Int = _signature.hashCode()
