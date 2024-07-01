package ecscalibur.core.component

import ecscalibur.error.IllegalDefinitionException
import scala.annotation.targetName

/** Type representing unique component IDs.
  */
opaque type ComponentId = Int

object ComponentId:
  final inline def Nil = ComponentId(-1)

  private[core] inline def apply(id: Int): ComponentId = id
  private[core] inline def apply(ids: Array[Int]): Array[ComponentId] = ids

  extension (id: ComponentId)
    private[core] inline def asInt: Int = id

  extension (arr: Array[ComponentId])
    private[core] inline def asIntArray: Array[Int] = arr

sealed trait WithType:
  protected val _typeId: Int = ComponentId.Nil.asInt

  def typeId: ComponentId = if _typeId != ComponentId.Nil.asInt then ComponentId(_typeId)
  else throw IllegalDefinitionException(s"$getClass must be annotated with @component.")

  /** Equivalent to 'typeId'.
    *
    * @return
    *   the type ID of this component.
    */
  @targetName("typeId")
  inline def unary_~ : ComponentId = typeId

trait Component extends WithType:
  inline infix def isA(compType: ComponentType): Boolean = typeId == compType.typeId

trait ComponentType extends WithType:
  override def equals(other: Any): Boolean = other match
    case o: ComponentType => typeId == o.typeId
    case _                => false
