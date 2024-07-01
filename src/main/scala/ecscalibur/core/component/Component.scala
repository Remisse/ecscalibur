package ecscalibur.core.component

/** Type representing unique component IDs.
  */
type ComponentId = Int

sealed trait WithType:
  final private val _typeId = tpe.getId(getClass)

  def typeId = _typeId

  /** Equivalent to 'typeId'.
    *
    * @return
    *   the type ID of this component.
    */
  final inline def unary_~ = typeId

trait Component extends WithType:
  final inline infix def isA(compType: ComponentType): Boolean = typeId == compType.typeId

trait ComponentType extends WithType:
  final override def equals(other: Any): Boolean = other match
    case o: ComponentType => typeId == o.typeId
    case _                => false

export TypeOrdering.given
object TypeOrdering:
  given Ordering[ComponentType] with
    override def compare(t1: ComponentType, t2: ComponentType): Int = t1.typeId - t2.typeId
