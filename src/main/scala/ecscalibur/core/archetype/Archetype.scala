package ecscalibur.core.archetype

import ecscalibur.core.component.*
import CSeq.Extensions.*
import ecscalibur.core.Entity
import ecscalibur.id.IdGenerator

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.*
import scala.reflect.ClassTag
import scala.annotation.targetName

private[core] object Archetypes:
  trait Archetype:
    def signature: Signature
    def add(e: Entity, entityComponents: CSeq): Unit
    def contains(e: Entity): Boolean
    def remove(e: Entity): CSeq
    def softRemove(e: Entity): Unit
    def readAll(predicate: ComponentId => Boolean)(f: (Entity, CSeq) => Unit): Unit
    def writeAll(predicate: ComponentId => Boolean)(f: (Entity, CSeq) => CSeq): Unit

  object Archetype:
    @targetName("fromTypes")
    def apply(types: ComponentType*): Archetype = apply(Signature(types*))
    @targetName("fromSignature")
    def apply(signature: Signature): Archetype = ArchetypeImpl(signature)

    private class ArchetypeImpl(inSignature: Signature) extends Archetype:
      import ecscalibur.core.util.array.*

      private val _signature: Signature = inSignature
      private val compIndexesByEntity: mutable.Map[Entity, Int] = mutable.HashMap.empty
      private val components: Map[ComponentId, ArrayBuffer[Component]] =
        _signature.underlying.aMap(t => t -> ArrayBuffer.empty[Component]).to(HashMap)
      private val idGenerator: IdGenerator = IdGenerator()

      override inline def signature: Signature = _signature

      override def add(e: Entity, entityComponents: CSeq): Unit =
        require(!contains(e), "Attempted to readd an already existing entity.")
        require(
          _signature == Signature(entityComponents.toTypes),
          "Given component types do not correspond to this archetype's signature."
        )
        val newEntityIdx = idGenerator.next
        compIndexesByEntity += e -> newEntityIdx
        entityComponents.underlying.aForeach: (c: Component) =>
          val compBuffer = components(c.typeId)
          if newEntityIdx >= compBuffer.length then compBuffer += c
          else compBuffer(newEntityIdx) = c

      override inline def contains(e: Entity): Boolean = 
        compIndexesByEntity.contains(e) && idGenerator.isValid(compIndexesByEntity(e))

      inline val removalErrorMsg = "Attempted to remove an entity not stored in this archetype."

      override def remove(e: Entity): CSeq =
        require(contains(e), removalErrorMsg)
        val idx = compIndexesByEntity(e)
        idGenerator.erase(idx) match
          case false => throw new IllegalArgumentException(removalErrorMsg)
          case _     => CSeq(components.map((_, comps) => comps(idx)))

      override def softRemove(e: Entity) =
        require(contains(e), removalErrorMsg)
        val idx = compIndexesByEntity(e)
        val _ = idGenerator.erase(idx)

      override def readAll(predicate: ComponentId => Boolean)(f: (Entity, CSeq) => Unit) =
        val filteredComps = components.filter((id, _) => predicate(id))
        for (e, idx) <- compIndexesByEntity if contains(e) do
          val inputComps = CSeq(filteredComps.map((_, comps) => comps(idx)))
          f(e, inputComps)

      override def writeAll(predicate: ComponentId => Boolean)(f: (Entity, CSeq) => CSeq) =
        val filteredComps = components.filter((id, _) => predicate(id))
        val inputIds = filteredComps.map((id, _) => id).toArray
        for (e, idx) <- compIndexesByEntity if contains(e) do
          val inputComps = CSeq(filteredComps.map((_, comps) => comps(idx)))
          val editedComponents: CSeq = f(e, inputComps)
          val returnedSignature = editedComponents.underlying.toSignature
          require(
            returnedSignature == inputIds.toSignature,
            s"Unexpected components returned.\nExpected: ${inputIds.mkString}\nFound: ${returnedSignature.underlying.mkString}"
          )
          editedComponents.underlying.aForeach: (c) =>
            components(c.typeId)(compIndexesByEntity(e)) = c

      override def equals(x: Any): Boolean = x match
        case a: Archetype => _signature == a.signature
        case _            => false

      override def hashCode(): Int = _signature.hashCode
