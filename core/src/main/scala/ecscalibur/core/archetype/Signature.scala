package ecscalibur.core.archetype

import ecscalibur.core.components.*

import scala.annotation.targetName
import ecsutil.array.*

/** Ordered sequence of distinct [[ComponentId]]s.
  *
  * @param underlying
  *   ComponentIds out of which this Signature will be made.
  */
private[ecscalibur] trait Signature(val underlying: Array[ComponentId]):
  /** Checks whether this Signature contains at least one of the ComponentIds the given Signature is
    * made of.
    *
    * @param other
    *   the Signature whose ComponentIds may or may not be included in this Signature
    * @return
    *   true if this Signature contains at least one of the ComponentIds the given Signature is made
    *   of, false otherwise.
    */
  final inline infix def containsAny(other: Signature): Boolean =
    other.underlying.aExists(underlying.aContains)

  /** Checks whether this Signature contains all of the ComponentIds the given Signature is made of.
    *
    * @param other
    *   the Signature whose ComponentIds may or may not be included in this Signature
    * @return
    *   true if this Signature contains all of the ComponentIds the given Signature is made of,
    *   false otherwise.
    */
  final inline infix def containsAll(other: Signature): Boolean =
    other.underlying.aForall(underlying.aContains)

  /** Checks whether this Signature contains all of the ComponentIds of the given types.
    *
    * @param types
    *   the types whose ComponentIds may or may not be included in this Signature
    * @return
    *   true if this Signature contains all of the ComponentIds of the given types, false otherwise.
    */
  final inline infix def containsAll(types: WithType*): Boolean =
    types.map(~_).forall(underlying.aContains)

  /** Checks whether this Signature is empty.
    *
    * @return
    *   true if this Signature is empty, false otherwise.
    */
  final inline def isNil: Boolean = underlying.isEmpty

  /**
    * Returns the length of this signature.
    *
    * @return
    *  the length of this signature.
    */
  final inline def length: Int = underlying.length

private[ecscalibur] final case class SignatureImpl(u: Array[ComponentId]) extends Signature(u):
  override def equals(other: Any): Boolean = other match
    case SignatureImpl(u) => underlying.aSameElements(u)
    case _                => false

  override def hashCode(): Int = java.util.Arrays.hashCode(underlying)

object Signature:
  /** Empty signature.
    */
  val Nil: Signature = new SignatureImpl(Array.empty[ComponentId])

  /** Creates a new Signature from the given ComponentIds.
    *
    * @param ids
    *   the ComponentIds this Signature will be made of
    * @throws IllegalArgumentException
    *   if the given sequence is empty or if it contains duplicate elements.
    * @return
    *   a new Signature instance.
    */
  @targetName("fromIds")
  def apply(ids: ComponentId*): Signature =
    require(ids.nonEmpty, "Failed to make signature: empty sequence.")
    val distinct = ids.distinct.toArray
    distinct.sortInPlace()
    new SignatureImpl(distinct)

  /** Creates a new Signature from the given Components or ComponentTypes.
    *
    * @param types
    *   the Components or ComponentTypes this Signature will be made of
    * @throws IllegalArgumentException
    *   if the given sequence is empty or if it contains duplicate elements.
    * @return
    *   a new Signature instance.
    */
  @targetName("fromTypes")
  inline def apply[T <: WithType](types: T*): Signature = apply(types.map(_.typeId)*)
