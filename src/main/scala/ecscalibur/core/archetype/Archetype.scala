package ecscalibur.core.archetype

import ecscalibur.core.Entity
import ecscalibur.core.component.*
import ecscalibur.id.IdGenerator
import ecscalibur.util.sizeof.sizeOf

import scala.annotation.tailrec
import scala.annotation.targetName
import scala.collection.immutable.*
import scala.collection.mutable
import scala.reflect.ClassTag

import CSeq.Extensions.*

private[core] object Archetypes:
  trait Archetype private[archetype] (val signature: Signature):
    def add(e: Entity, entityComponents: CSeq): Unit
    def contains(e: Entity): Boolean
    def remove(e: Entity): CSeq
    def softRemove(e: Entity): Unit
    def iterate(selectedIds: Signature, rwIds: Array[ComponentId])(f: (Entity, CSeq) => Unit): Unit

  object Archetype:
    // TODO Make this parameter configurable
    inline val DefaultFragmentSizeBytes = 16384
    @targetName("fromTypes")
    def apply(types: ComponentType*): Aggregate =
      Aggregate(Signature(types*))(DefaultFragmentSizeBytes)
    @targetName("fromSignature")
    def apply(signature: Signature): Aggregate = Aggregate(signature)(DefaultFragmentSizeBytes)

  trait Aggregate extends Archetype:
    def fragments: Iterable[Fragment]

  trait Fragment extends Archetype:
    def isFull: Boolean
    def isEmpty: Boolean
    def update(e: Entity, c: Component): Unit

  object Aggregate:
    @targetName("fromTypes")
    def apply(types: ComponentType*)(maxFragmentSizeBytes: Long): Aggregate =
      apply(Signature(types*))(maxFragmentSizeBytes)
    @targetName("fromSignature")
    def apply(signature: Signature)(maxFragmentSizeBytes: Long): Aggregate =
      AggregateImpl(signature, maxFragmentSizeBytes)

    private class AggregateImpl(inSignature: Signature, maxFragmentSizeBytes: Long)
        extends Archetype(inSignature),
          Aggregate:
      import ecscalibur.util.array.*

      private var _fragments: Vector[Fragment] = Vector.empty
      private val entitiesByFragment: mutable.Map[Entity, Fragment] = mutable.Map.empty

      override def fragments: Iterable[Fragment] = _fragments

      override def add(e: Entity, entityComponents: CSeq): Unit =
        require(!contains(e), "Attempted to readd an already existing entity.")
        require(
          signature == Signature(entityComponents.toTypes),
          "Given component types do not correspond to this archetype's signature."
        )
        if (_fragments.isEmpty || _fragments.head.isFull)
          val sizeBytes = estimateComponentsSize(entityComponents)
          if (sizeBytes > maxFragmentSizeBytes)
            throw IllegalStateException(
              s"Exceeded the maximum fragment size ($sizeBytes > $maxFragmentSizeBytes)."
            )
          val maxEntities = (maxFragmentSizeBytes / sizeBytes).toInt
          _fragments = Fragment(signature, maxEntities) +: _fragments
        _fragments.head.add(e, entityComponents)
        entitiesByFragment += e -> _fragments.head

      private inline def estimateComponentsSize(components: CSeq): Long =
        var estimatedComponentsSize: Long = 0
        components.underlying.aForeach(c => estimatedComponentsSize += sizeOf(c))
        estimatedComponentsSize

      override inline def contains(e: Entity): Boolean = entitiesByFragment.contains(e)

      inline val removalErrorMsg = "Attempted to remove an entity not stored in this archetype."

      override def remove(e: Entity): CSeq =
        require(contains(e), removalErrorMsg)
        val fragment = entitiesByFragment(e)
        entitiesByFragment -= e
        val res = fragment.remove(e)
        maybeDeleteFragment(fragment)
        res

      override def softRemove(e: Entity) =
        require(contains(e), removalErrorMsg)
        val fragment = entitiesByFragment(e)
        entitiesByFragment -= e
        fragment.softRemove(e)
        maybeDeleteFragment(fragment)

      private inline def maybeDeleteFragment(fr: Fragment) =
        if (fr.isEmpty && fr != _fragments.head)
          _fragments = _fragments.filterNot(_ == fr)

      override def iterate(selectedIds: Signature, rwIds: Array[ComponentId])(f: (Entity, CSeq) => Unit) = 
        _fragments.foreach(fr => fr.iterate(selectedIds, rwIds)(f))

      override def equals(x: Any): Boolean = x match
        case a: Archetype => signature == a.signature
        case _            => false

      override def hashCode(): Int = signature.hashCode

  object Fragment:
    def apply(signature: Signature, maxEntities: Int): Fragment =
      FragmentImpl(signature, maxEntities)

    private class FragmentImpl(inSignature: Signature, maxEntities: Int)
        extends Archetype(inSignature),
          Fragment:
      import ecscalibur.util.array.*

      private val idGenerator: IdGenerator = IdGenerator()
      private val entityIndexes: mutable.Map[Entity, Int] =
        new mutable.HashMap(maxEntities, FragmentImpl.LoadFactor)
      private val components: Map[ComponentId, CSeq] =
        signature.underlying.aMap(t => t -> CSeq(Array.ofDim[Component](maxEntities))).to(HashMap)

      override inline def isFull: Boolean = entityIndexes.size == maxEntities

      override def isEmpty: Boolean = entityIndexes.isEmpty

      override def update(e: Entity, c: Component): Unit =
        components(c.typeId)(entityIndexes(e)) = c

      override def add(e: Entity, entityComponents: CSeq): Unit =
        // No need to validate the inputs, as Archetype already takes care of it.
        require(!isFull, s"Cannot add more entities beyond the maximum limit ($maxEntities).")
        val newEntityIdx = idGenerator.next
        entityIndexes += e -> newEntityIdx
        entityComponents.underlying.aForeach: (c: Component) =>
          components(c.typeId)(newEntityIdx) = c

      override inline def contains(e: Entity): Boolean =
        entityIndexes.contains(e)

      inline val removalErrorMsg = "Attempted to remove an entity not stored in this archetype."

      override def remove(e: Entity): CSeq =
        val idx = removeEntityFromMap(e)
        idGenerator.erase(idx) match
          case false => throw new IllegalArgumentException(removalErrorMsg)
          case _     => CSeq(components.map((_, comps) => comps(idx)))

      override def softRemove(e: Entity) =
        val idx = removeEntityFromMap(e)
        val _ = idGenerator.erase(idx)

      private inline def removeEntityFromMap(e: Entity): Int =
        val idx = entityIndexes(e)
        entityIndexes -= e
        idx

      override def iterate(selectedIds: Signature, rwIds: Array[ComponentId])(f: (Entity, CSeq) => Unit) = 
        for (e, idx) <- entityIndexes do
          val entityComps = Array.ofDim[Component](selectedIds.underlying.length)

          @tailrec
          def gatherComponentsOfEntity(i: Int): Unit = i match
            case -1 => ()
            case _ =>
              val componentId = selectedIds.underlying(i)
              val component = components(componentId)(idx)
              entityComps(i) = 
                if (rwIds.aContains(componentId)) Rw(component)(this, e)
                else component
              gatherComponentsOfEntity(i - 1)
          
          gatherComponentsOfEntity(entityComps.length - 1)
          f(e, CSeq(entityComps))
          
    object FragmentImpl:
      val LoadFactor = 0.7
