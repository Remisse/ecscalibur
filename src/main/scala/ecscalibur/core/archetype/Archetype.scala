package ecscalibur.core.archetype

import ecscalibur.core.component.*
import CSeq.Extensions.*
import ecscalibur.core.Entity
import ecscalibur.id.IdGenerator
import ecscalibur.util.sizeOf.sizeOf

import scala.annotation.targetName
import scala.collection.mutable
import scala.collection.immutable.*
import scala.reflect.ClassTag
import scala.annotation.tailrec

private[core] object Archetypes:
  trait Archetype private[archetype] (val signature: Signature):
    def add(e: Entity, entityComponents: CSeq): Unit
    def contains(e: Entity): Boolean
    def remove(e: Entity): CSeq
    def softRemove(e: Entity): Unit
    def iterate(predicate: ComponentId => Boolean)(f: (Entity, CSeq) => CSeq): Unit

  object Archetype:
    // TODO Make this parameter configurable
    inline val DefaultFragmentSizeBytes = 16384
    @targetName("fromTypes")
    def apply(types: ComponentType*): Aggregate = Aggregate(DefaultFragmentSizeBytes, Signature(types*))
    @targetName("fromSignature")
    def apply(signature: Signature): Aggregate = Aggregate(DefaultFragmentSizeBytes, signature)

  trait Aggregate extends Archetype:
    def fragments: Iterable[Fragment]

  trait Fragment extends Archetype:
    def isFull: Boolean
    def isEmpty: Boolean

  object Aggregate:
    @targetName("fromTypes")
    def apply(maxFragmentSizeBytes: Long, types: ComponentType*): Aggregate = apply(maxFragmentSizeBytes, Signature(types*))
    @targetName("fromSignature")
    def apply(maxFragmentSizeBytes: Long, signature: Signature): Aggregate = AggregateImpl(signature, maxFragmentSizeBytes)

    private class AggregateImpl(inSignature: Signature, maxFragmentSizeBytes: Long) extends Archetype(inSignature), Aggregate:
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

      override def iterate(predicate: ComponentId => Boolean)(f: (Entity, CSeq) => CSeq) =
        _fragments.foreach(fr => fr.iterate(predicate)(f))

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
      private val entityIndexes: mutable.Map[Entity, Int] = new mutable.HashMap(maxEntities, FragmentImpl.LoadFactor)
      private val components: Map[ComponentId, CSeq] =
        signature.underlying.aMap(t => t -> CSeq(Array.ofDim[Component](maxEntities))).to(HashMap)

      override inline def isFull: Boolean = entityIndexes.size == maxEntities

      override def isEmpty: Boolean = entityIndexes.isEmpty

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

      override def iterate(predicate: ComponentId => Boolean)(f: (Entity, CSeq) => CSeq) =
        val filteredComps = components.collect { case (id, a) if predicate(id) => a }
        for (e, idx) <- entityIndexes do
          val inputComps = Array.ofDim[Component](filteredComps.size)

          @tailrec
          def gatherComponentsOfEntity(i: Int, comps: Iterable[CSeq]): Unit = i match
            case -1 => ()
            case _ =>
              inputComps(i) = comps.head(idx)
              gatherComponentsOfEntity(i - 1, comps.tail)

          gatherComponentsOfEntity(inputComps.length - 1, filteredComps)
          val editedComponents: CSeq = f(e, CSeq(inputComps))
          val returnedSignature =
            if editedComponents.underlying.isEmpty then Signature.Nil
            else editedComponents.underlying.toSignature
          val inputIds = inputComps.aMap(~_)
          require(
            returnedSignature.isNil || Signature(inputIds).containsAll(returnedSignature),
            s"Unexpected components returned.\nExpected: ${inputIds.mkString}\nFound: ${returnedSignature.underlying.mkString}"
          )
          editedComponents.underlying.aForeach: (c) =>
            components(c.typeId)(entityIndexes(e)) = c

    object FragmentImpl:
      val LoadFactor = 0.7
