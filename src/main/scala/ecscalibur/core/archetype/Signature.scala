package ecscalibur.core.archetype

import ecscalibur.core.Components.{WithType, ComponentId}
import scala.annotation.targetName

object Signatures:
  case class Signature private[Signatures] (val underlying: Array[ComponentId]):
    inline infix def isPartOf(other: Signature): Boolean = other.underlying.containsSlice(underlying)

    inline infix def containsAny(other: Signature): Boolean = other.underlying.exists(underlying.contains)

    inline infix def containsAll(other: Signature): Boolean = other.underlying.forall(underlying.contains)

    inline def isNil: Boolean = underlying.isEmpty

    override def equals(other: Any): Boolean = other match
      case Signature(u) => underlying.sameElements(u)
      case _ => false

    override def hashCode(): Int = java.util.Arrays.hashCode(underlying)

  object Signature:
    val nil: Signature = new Signature(Array.empty[ComponentId])

    @targetName("fromIds")
    def apply(ids: ComponentId*): Signature =
      require(ids.nonEmpty, "Failed to make signature: empty sequence.")
      val res = ids.distinct.sorted
      require(res.length == ids.length, "Duplicate types found.")
      new Signature(res.toArray)

    @targetName("fromIds")
    def apply(ids: Array[ComponentId]): Signature =
      require(ids.nonEmpty, "Failed to make signature: empty sequence.")
      val res = ids.distinct.sorted
      require(res.length == ids.length, "Duplicate types found.")
      new Signature(res)

    @targetName("fromTypes")
    def apply[T <: WithType](types: T*): Signature =
      apply(types.map(_.typeId).toArray)

    @targetName("fromTypes")
    def apply[T <: WithType](types: Array[T]): Signature =
      apply(types.map(_.typeId))

    object Extensions:
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
