package ecscalibur.core.archetype

import ecscalibur.core.component.ComponentId
import ecscalibur.core.component.WithType
import ecsutil.array._

import scala.annotation.targetName

/** Ordered sequence of distinct [[ComponentId]]s.
  *
  * @param underlying
  *   ComponentIds out of which this Signature will be made.
  */
private[ecscalibur] final case class Signature(val underlying: Array[ComponentId]):
  /** Checks whether this Signature contains at least one of the ComponentIds the given
    * Signature is made of.
    *
    * @param other
    *   the Signature whose ComponentIds may or may not be included in this Signature
    * @return
    *   true if this Signature contains at least one of the ComponentIds the given Signature is made
    *   of, false otherwise.
    */
  inline infix def containsAny(other: Signature): Boolean =
    other.underlying.aExists(underlying.aContains)

  /** Checks whether this Signature contains all of the ComponentIds the given Signature is
    * made of.
    *
    * @param other
    *   the Signature whose ComponentIds may or may not be included in this Signature
    * @return
    *   true if this Signature contains all of the ComponentIds the given Signature is made of,
    *   false otherwise.
    */
  inline infix def containsAll(other: Signature): Boolean =
    other.underlying.aForall(underlying.aContains)

  /** Checks whether this Signature is empty.
    *
    * @return
    *   true if this Signature is empty, false otherwise.
    */
  inline def isNil: Boolean = underlying.isEmpty

  override def equals(other: Any): Boolean = other match
    case Signature(u) => underlying.aSameElements(u)
    case _            => false

  override def hashCode(): Int = java.util.Arrays.hashCode(underlying)

object Signature:
  /** Empty signature.
    */
  val Nil: Signature = new Signature(Array.empty[ComponentId])

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
  def apply(ids: Array[ComponentId]): Signature =
    require(ids.nonEmpty, "Failed to make signature: empty sequence.")
    val res: Array[Int] = ids.distinct.sortInPlace().array
    require(res.length == ids.length, "Duplicate types found.")
    new Signature(ComponentId(res))

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
  inline def apply(ids: ComponentId*): Signature = apply(ids.toArray)

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
  inline def apply[T <: WithType](types: T*): Signature = apply(types.map(_.typeId).toArray)

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
  inline def apply[T <: WithType](types: Array[T]): Signature = apply(types.aMap(_.typeId))
