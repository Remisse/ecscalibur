package ecscalibur.core.archetype

import ecscalibur.core.component.{ComponentId, WithType}
import scala.annotation.targetName
import ecscalibur.util.array.*

case class Signature private (val underlying: Array[ComponentId]):
  inline infix def isPartOf(other: Signature): Boolean = other.underlying.aContainsSlice(underlying)

  inline infix def containsAny(other: Signature): Boolean = other.underlying.aExists(underlying.aContains)

  inline infix def containsAll(other: Signature): Boolean = other.underlying.aForall(underlying.aContains)

  inline def isNil: Boolean = underlying.isEmpty

  override def equals(other: Any): Boolean = other match
    case Signature(u) => underlying.aSameElements(u)
    case _ => false

  override def hashCode(): Int = java.util.Arrays.hashCode(underlying)

export Signature.Extensions.*
object Signature:
  val Nil: Signature = new Signature(Array.empty[ComponentId])

  @targetName("fromIds")
  def apply(ids: Array[ComponentId]): Signature =
    require(ids.nonEmpty, "Failed to make signature: empty sequence.")
    val res = ids.distinct.sorted
    require(res.length == ids.length, "Duplicate types found.")
    new Signature(res)

  @targetName("fromIds")
  inline def apply(ids: ComponentId*): Signature = apply(ids.toArray)

  @targetName("fromTypes")
  inline def apply[T <: WithType](types: T*): Signature = apply(types.map(_.typeId).toArray)

  @targetName("fromTypes")
  inline def apply[T <: WithType](types: Array[T]): Signature = apply(types.map(_.typeId))

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
