package ecscalibur.core.component

/** Type representing unique component IDs.
  */
opaque type ComponentId = Int

object ComponentId:
  private[core] inline def apply(id: Int): ComponentId = id
  private[core] inline def apply(ids: Array[Int]): Array[ComponentId] = ids

  extension (id: ComponentId)
    private[core] inline def asInt: Int = id

  extension (arr: Array[ComponentId])
    private[core] inline def asIntArray: Array[Int] = arr

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
