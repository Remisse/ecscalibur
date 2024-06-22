package ecscalibur.core.archetype

import ecscalibur.core.Components.{WithType, ComponentId}
import scala.annotation.targetName

opaque type Signature = Array[ComponentId]

object Signature:
  val nil: Signature = Array.empty

  @targetName("fromIds")
  inline def apply(ids: ComponentId*): Signature =
    require(ids.nonEmpty, "Failed to make signature: empty sequence.")
    val res = ids.distinct.sorted
    require(res.length == ids.length, "Duplicate types found.")
    res.toArray

  @targetName("fromIds")
  inline def apply(ids: Array[ComponentId]): Signature =
    require(ids.nonEmpty, "Failed to make signature: empty sequence.")
    val res = ids.distinct.sorted
    require(res.length == ids.length, "Duplicate types found.")
    res

  @targetName("fromTypes")
  inline def apply[T <: WithType](types: T*): Signature =
    apply(types.map(_.typeId).toArray)

  @targetName("fromTypes")
  inline def apply[T <: WithType](types: Array[T]): Signature =
    apply(types.map(_.typeId))

  object Extensions:
    extension (s: Signature)
      /** Makes this signature accessible as an array of [[ComponentId]]s. Performance cost is
        * minimal, if not outright nonexistent.
        *
        * @return
        *   an array of [[ComponentId]]s representing this signature.
        */
      inline def underlying: Array[ComponentId] = s

      /** Checks for equality between two signatures. Use this instead of 'equals' or '=='.
        *
        * @param x
        * @return
        */
      inline infix def sameAs(other: Signature): Boolean = s.sameElements(other)

      inline infix def isPartOf(other: Signature): Boolean = other.containsSlice(s)

      inline infix def containsAny(other: Signature): Boolean = other.exists(s.contains)

      inline def isNil: Boolean = s.isEmpty

    extension (ids: ComponentId*)
      @targetName("idsToSignature")
      inline def toSignature = Signature(ids*)

    extension (ids: Array[ComponentId])
      @targetName("idsToSignature")
      inline def toSignature = Signature(ids)

    extension [T <: WithType](types: T*)
      @targetName("typesToSignature")
      inline def toSignature = Signature(types*)

    extension [T <: WithType](types: Array[T])
      @targetName("typesToSignature")
      inline def toSignature = Signature(types)

export Signature.Extensions.*
