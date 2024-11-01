package ecscalibur.core.archetype

import ecscalibur.core.components.*
import ecscalibur.core.entity.Entity
import ecsutil.ProgressiveMap

import scala.annotation.targetName
import scala.collection.immutable.*
import scala.collection.mutable
import scala.reflect.ClassTag

private[ecscalibur] object archetypes:
  /** A collection of Entities all featuring a specific combination of [[Component]]s.
    *
    * @param signature
    *   this Archetype's [[Signature]].
    */
  sealed trait Archetype private[archetype] (val signature: Signature):
    /** Adds an [[Entity]] to this Archetype along with the given [[Component]]s. The sequence of
      * components must satisfy this Archetype's [[Signature]].
      *
      * @param e
      *   the Entity to be added
      * @throws IllegalArgumentException
      *   if the given components do not satisfy this archetype's signature.
      * @param entityComponents
      *   the Components to be added
      * @return
      *   this Archetype
      */
    def add(e: Entity, entityComponents: Component*): Unit

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
    def remove(e: Entity): Array[Component]

    /** Same as [[Archetype.remove]], but does not return anything.
      *
      * @param e
      *   the Entity to be removed
      * @throws IllegalArgumentException
      *   if the given Entity is not stored in this Archetype.
      */
    def softRemove(e: Entity): Unit

    // TODO Add ProgressiveMap[ComponentId] as a lambda parameter to avoid scanning the whole array
    // for the various component classes
    /** Iterates over all entities stored in this Archetype and calls the given function on them and
      * all of their Components.
      *
      * @param f
      *   the function to be applied to all Entities and their Components
      */
    def iterate(f: (Entity, Array[Component]) => Unit): Unit

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
    inline val DefaultFragmentSize = 32

    /** Creates an [[Aggregate]] archetype with the given [[Signature]].
      *
      * @param signature
      *   the Signature of this new archetype
      * @return
      *   a new instance of [[Aggregate]]
      */
    @targetName("fromSignature")
    def apply(signature: Signature): Aggregate = Aggregate(signature)(DefaultFragmentSize)

  /** Models an Archetype made of multiple [[Fragment]]s
    */
  trait Aggregate extends Archetype:

    /** @return
      *   an Iterator over the [[Fragment]]s stored in this Aggregate archetype.
      */
    def fragments: Iterator[Fragment]

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

  object Aggregate:
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

    private final class AggregateImpl(inSignature: Signature, maxFragmentSize: Long)
        extends Archetype(inSignature),
          Aggregate:
      import scala.collection.mutable.ArrayBuffer

      require(inSignature != Signature.Nil, "Attempted to create an Archetype with no signature.")

      private val _fragments: ArrayBuffer[Fragment] = ArrayBuffer.empty
      private val fragmentsByEntity: mutable.Map[Entity, Fragment] = mutable.Map.empty
      private val componentMappings =
        ProgressiveMap.from[ComponentId](inSignature.underlying.toArray*)

      override def fragments: Iterator[Fragment] = _fragments.iterator

      override def add(e: Entity, components: Component*): Unit =
        val fr =
          if _fragments.isEmpty || lastFragment.isFull then appendNewFragment() else lastFragment
        fr.add(e, components*)
        fragmentsByEntity += e -> fr

      private inline def lastFragment: Fragment = _fragments.last

      private inline def appendNewFragment(): Fragment =
        val newFragment = Fragment(signature, componentMappings, maxFragmentSize.toInt)
        _fragments += newFragment
        newFragment

      override inline def contains(e: Entity): Boolean = fragmentsByEntity.contains(e)

      override def remove(e: Entity): Array[Component] =
        val fragment = removeFromMapAndGetFormerFragment(e)
        val res = fragment.remove(e)
        maybeDeleteFragment(fragment)
        res

      override def softRemove(e: Entity): Unit =
        val fragment = removeFromMapAndGetFormerFragment(e)
        fragment.softRemove(e)
        maybeDeleteFragment(fragment)

      private inline def removeFromMapAndGetFormerFragment(e: Entity): Fragment =
        val fragment = fragmentsByEntity(e)
        fragmentsByEntity -= e
        fragment

      private inline def maybeDeleteFragment(fr: Fragment): Unit =
        if fr.isEmpty && fr != lastFragment then _fragments -= fr

      override def iterate(f: (Entity, Array[Component]) => Unit): Unit =
        _fragments.foreach(_.iterate(f))

      override def update(e: Entity, c: Component): Unit =
        fragmentsByEntity(e).update(e, c)

      override def equals(x: Any): Boolean = x match
        case a: Aggregate => signature == a.signature
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
    def apply(
        signature: Signature,
        mappings: ProgressiveMap[ComponentId],
        maxEntities: Int
    ): Fragment =
      FragmentImpl(signature, mappings, maxEntities)

    private final class FragmentImpl(
        inSignature: Signature,
        componentMappings: ProgressiveMap[ComponentId],
        maxEntities: Int
    ) extends Archetype(inSignature),
          Fragment:
      private val entities: Array[Entity] = Array.fill[Entity](maxEntities)(Entity.Nil)
      private val components: Array[Array[Component]] =
        Array.fill[Array[Component]](maxEntities)(
          Array.ofDim[Component](inSignature.length)
        )
      private var effectiveSize = 0

      override inline def isFull: Boolean = effectiveSize == maxEntities

      override def isEmpty: Boolean = effectiveSize == 0

      import ecsutil.array.*

      override def update(e: Entity, c: Component): Unit =
        updateInline(e, c)

      override def add(e: Entity, entityComponents: Component*): Unit =
        val firstAvailableIdx = entities.aIndexWhere(_ == Entity.Nil)
        entities(firstAvailableIdx) = e
        for c <- entityComponents do updateInline(e, c)
        effectiveSize += 1

      private inline def updateInline(e: Entity, c: Component): Unit =
        components(entityIndex(e))(componentIndex(c)) = c

      override inline def contains(e: Entity): Boolean =
        entityIndex(e) != -1

      override def remove(e: Entity): Array[Component] =
        components(removeEntityFromList(e))

      override def softRemove(e: Entity): Unit =
        val _ = removeEntityFromList(e)

      override def iterate(f: (Entity, Array[Component]) => Unit): Unit =
        @annotation.tailrec
        def _iterate(i: Int): Unit = i match
          case i if i == maxEntities => ()
          case i =>
            if entities(i) != Entity.Nil then f(entities(i), components(i))
            _iterate(i + 1)

        _iterate(0)

      private inline def removeEntityFromList(e: Entity): Int =
        val eIndex = entityIndex(e)
        entities(eIndex) = Entity.Nil
        effectiveSize -= 1
        eIndex

      private inline def entityIndex(e: Entity): Int = entities.aIndexOf(e)

      private inline def componentIndex(c: Component): Int = componentMappings(c.typeId)
