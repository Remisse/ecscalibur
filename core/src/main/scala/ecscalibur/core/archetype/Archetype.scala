package ecscalibur.core.archetype

import ecscalibur.core.CSeq
import ecscalibur.core.Entity
import ecscalibur.core.component._
import ecscalibur.id.IdGenerator
import ecscalibur.util.sizeof.sizeOf

import scala.annotation.tailrec
import scala.annotation.targetName
import scala.collection.immutable._
import scala.collection.mutable
import scala.reflect.ClassTag

import CSeq._

private[ecscalibur] object archetypes:
  /** A collection of Entities all featuring a specific combination of [[Component]]s.
    *
    * @param signature
    *   this Archetype's [[Signature]].
    */
  trait Archetype private[archetype] (val signature: Signature):
    /** Adds an [[Entity]] to this Archetype along with the given [[Component]]s. The sequence of
      * components must satisfy this Archetypes's [[Signature]].
      *
      * @param e
      *   the Entity to be added
      * @throws IllegalArgumentException
      *   if the given components do not satisfy this archetype's signature.
      * @param entityComponents
      *   the Components to be added
      */
    def add(e: Entity, entityComponents: CSeq[Component]): Unit

    /** Checks whether the given Entity is stored in this Archetype.
      *
      * @param e
      *   the Entity to perform the check on
      * @return
      *   true if this Archetype contains the given Entity, false otherwise.
      */
    def contains(e: Entity): Boolean

    /** Removes the given Entity from this Archetype, if present, and returns all of its Components.
      *
      * @param e
      *   the Entity to be removed
      * @throws IllegalArgumentException
      *   if the given Entity is not stored in this Archetype.
      * @return
      *   the given Entity's Components.
      */
    def remove(e: Entity): CSeq[Component]

    /** Same as [[Archetype.remove]], but does not return anything.
      *
      * @param e
      *   the Entity to be removed
      * @throws IllegalArgumentException
      *   if the given Entity is not stored in this Archetype.
      */
    def softRemove(e: Entity): Unit

    /** Iterates over all entities stored in this Archetype and calls the given function on them and
      * all of their Components whose [[ComponentId]]s are part of the given [[Signature]].
      *
      * @param selectedIds
      *   the ComponentIds of the Components that must be iterated over
      * @param f
      *   the function to be applied to all Entities and their Components
      */
    def iterate(selectedIds: Signature)(f: (Entity, CSeq[Component], Archetype) => Unit): Unit

    /** Replaces the given Entity's Component with the given value.
      *
      * @param e
      *   the Entity on which to perform the update
      * @throws IllegalArgumentException
      *   if the given Entity is not stored in this Archetype or the given Component is not part of
      *   this Archetype's signature.
      * @param c
      *   the Component to be replaced
      */
    def update(e: Entity, c: Component): Unit

  object Archetype:
    // TODO Make this parameter configurable
    /** Default maximum size of a [[Fragment]].
      */
    inline val DefaultFragmentSizeBytes = 16384

    /** Creates an [[Aggregate]] archetype with a [[Signature]] derived from the given types.
      *
      * @param types
      *   [[ComponentType]]s from which the archetype's Signature must be derived
      * @return
      *   a new instance of [[Aggregate]]
      */
    @targetName("fromTypes")
    def apply(types: ComponentType*): Aggregate =
      Aggregate(Signature(types*))(DefaultFragmentSizeBytes)

    /** Creates an [[Aggregate]] archetype with the given [[Signature]].
      *
      * @param signature
      *   the Signature of this new archetype
      * @return
      *   a new instance of [[Aggregate]]
      */
    @targetName("fromSignature")
    def apply(signature: Signature): Aggregate = Aggregate(signature)(DefaultFragmentSizeBytes)

  /** Models an Archetype made of multiple [[Fragment]]s
    */
  trait Aggregate extends Archetype:

    /** @return
      *   all [[Fragment]]s stored in this Aggregate archetype.
      */
    def fragments: Iterable[Fragment]

  /** Models an Archetype that can store a fixed number of entities and components.
    */
  trait Fragment extends Archetype:

    /** @return
      *   true if this Fragment cannot store any more Entities and Components, false otherwise.
      */

    def isFull: Boolean

    /** @return
      *   true if this Fragment does not contain any Entities or Components, false otherwise.
      */
    def isEmpty: Boolean

  private inline val removalErrorMsg = "Attempted to remove an entity not stored in this archetype."

  object Aggregate:
    /** Creates an Aggregate instance with a Signature derived from the given types and with a
      * maximum Fragment size specified by maxFragmentSizeBytes.
      *
      * @param types
      *   [[ComponentType]]s from which the archetype's Signature must be derived
      * @param maxFragmentSizeBytes
      *   the maximum size of all Fragments created by this Aggregate
      * @return
      *   a new Aggregate instance
      */
    @targetName("fromTypes")
    def apply(types: ComponentType*)(maxFragmentSizeBytes: Long): Aggregate =
      apply(Signature(types*))(maxFragmentSizeBytes)

    /** Creates an Aggregate instance with the given Signature and with a maximum Fragment size
      * specified by maxFragmentSizeBytes.
      *
      * @param signature
      *   the Signature of this new archetype
      * @param maxFragmentSizeBytes
      *   the maximum size of all Fragments created by this Aggregate
      * @return
      *   a new Aggregate instance
      */
    @targetName("fromSignature")
    def apply(signature: Signature)(maxFragmentSizeBytes: Long): Aggregate =
      AggregateImpl(signature, maxFragmentSizeBytes)

    private final class AggregateImpl(inSignature: Signature, maxFragmentSizeBytes: Long)
        extends Archetype(inSignature),
          Aggregate:
      import ecscalibur.util.array.*
      private var _fragments: Vector[Fragment] = Vector.empty
      private val fragmentsByEntity: mutable.Map[Entity, Fragment] = mutable.Map.empty

      override def fragments: Iterable[Fragment] = _fragments

      override def add(e: Entity, entityComponents: CSeq[Component]): Unit =
        require(!contains(e), "Attempted to add an already existing entity.")
        require(
          signature == Signature(entityComponents.map(~_).toArray),
          "Given component types do not correspond to this archetype's signature."
        )
        if (_fragments.isEmpty || _fragments.head.isFull) prependNewFragment(entityComponents)
        _fragments.head.add(e, entityComponents)
        fragmentsByEntity += e -> _fragments.head

      private inline def prependNewFragment(components: CSeq[Component]): Unit =
        val sizeBytes = estimateComponentsSize(components)
        if (sizeBytes > maxFragmentSizeBytes)
          throw IllegalStateException(
            s"Exceeded the maximum fragment size ($sizeBytes > $maxFragmentSizeBytes)."
          )
        val maxEntities = (maxFragmentSizeBytes / sizeBytes).toInt
        _fragments = Fragment(signature, maxEntities) +: _fragments

      private inline def estimateComponentsSize(components: CSeq[Component]): Long =
        var estimatedComponentsSize: Long = 0
        components.foreach(c => estimatedComponentsSize += sizeOf(c))
        estimatedComponentsSize

      override inline def contains(e: Entity): Boolean = fragmentsByEntity.contains(e)

      override def remove(e: Entity): CSeq[Component] =
        val fragment = removeFromMapAndGetFormerFragment(e)
        val res = fragment.remove(e)
        maybeDeleteFragment(fragment)
        res

      override def softRemove(e: Entity): Unit =
        val fragment = removeFromMapAndGetFormerFragment(e)
        fragment.softRemove(e)
        maybeDeleteFragment(fragment)

      private inline def removeFromMapAndGetFormerFragment(e: Entity): Fragment =
        require(contains(e), removalErrorMsg)
        val fragment = fragmentsByEntity(e)
        fragmentsByEntity -= e
        fragment

      private inline def maybeDeleteFragment(fr: Fragment): Unit =
        if (fr.isEmpty && fr != _fragments.head)
          _fragments = _fragments.filterNot(_ == fr)

      override def iterate(selectedIds: Signature)(
          f: (Entity, CSeq[Component], Archetype) => Unit
      ): Unit =
        _fragments.foreach(fr => fr.iterate(selectedIds)(f))

      override def update(e: Entity, c: Component): Unit =
        require(fragmentsByEntity.contains(e), "Given entity is not stored in this archetype.")
        require(
          signature.underlying.contains(c.typeId),
          "Attempted to update the value of a non-existing component."
        )
        fragmentsByEntity(e).update(e, c)

      override def equals(x: Any): Boolean = x match
        case a: Archetype => signature == a.signature
        case _            => false

      override def hashCode(): Int = signature.hashCode

  object Fragment:

    /** Creates a new [[Fragment]] instance with the given Signature and a maximum number of
      * entities that can be stored.
      *
      * @param signature
      *   the Signature of this new Fragment
      * @param maxEntities
      *   the maximum number of Entities that can be stored in this Fragment
      * @return
      *   a new Fragment instance
      */
    def apply(signature: Signature, maxEntities: Int): Fragment =
      FragmentImpl(signature, maxEntities)

    private final class FragmentImpl(inSignature: Signature, maxEntities: Int)
        extends Archetype(inSignature),
          Fragment:
      import ecscalibur.util.array.*

      private val idGenerator: IdGenerator = IdGenerator()
      private val entityIndexes: mutable.Map[Entity, Int] =
        new mutable.HashMap(maxEntities, FragmentImpl.LoadFactor)
      private val components: Map[ComponentId, CSeq[Component]] =
        signature.underlying
          .aMap(_ -> CSeq.ofDim[Component](maxEntities))
          .to(HashMap)

      override inline def isFull: Boolean = entityIndexes.size == maxEntities

      override def isEmpty: Boolean = entityIndexes.isEmpty

      override def update(e: Entity, c: Component): Unit =
        components(c.typeId)(entityIndexes(e)) = c

      override def add(e: Entity, entityComponents: CSeq[Component]): Unit =
        // No need to validate the inputs, as Aggregate already takes care of it.
        require(!isFull, s"Cannot add more entities beyond the maximum limit ($maxEntities).")
        val newEntityIdx = idGenerator.next
        entityIndexes += e -> newEntityIdx
        entityComponents.foreach: (c: Component) =>
          components(c.typeId)(newEntityIdx) = c

      override inline def contains(e: Entity): Boolean =
        entityIndexes.contains(e)

      override def remove(e: Entity): CSeq[Component] =
        val idx = removeEntityFromMap(e)
        idGenerator.erase(idx) match
          case false => throw new IllegalArgumentException(removalErrorMsg)
          case _     => CSeq[Component](components.map((_, comps) => comps(idx)))

      override def softRemove(e: Entity): Unit =
        val idx = removeEntityFromMap(e)
        val _ = idGenerator.erase(idx)

      private inline def removeEntityFromMap(e: Entity): Int =
        val idx = entityIndexes(e)
        entityIndexes -= e
        idx

      override def iterate(selectedIds: Signature)(
          f: (Entity, CSeq[Component], Archetype) => Unit
      ): Unit =
        for (e, entityIdx) <- entityIndexes do
          val entityComps = CSeq.ofDim[Component](selectedIds.underlying.length)

          @tailrec
          def gatherComponentsOfEntity(i: Int): Unit = i match
            case i if i == entityComps.length => ()
            case _ =>
              val componentId = selectedIds.underlying(i)
              entityComps(i) = components(componentId)(entityIdx)
              gatherComponentsOfEntity(i + 1)

          gatherComponentsOfEntity(0)
          f(e, entityComps, this)

    private object FragmentImpl:
      private val LoadFactor = 0.7
