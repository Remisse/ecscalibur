package ecscalibur.core

import ecscalibur.error.IllegalDefinitionException

import scala.annotation.targetName

export components.*

object components:
  /** Type representing unique component IDs.
    */
  type ComponentId = Int

  object ComponentId:
    /** Invalid ComponentId.
      *
      * @return
      *   an invalid ComponentId
      */
    final inline def Nil: ComponentId = ComponentId(-1)

    /** Creates a new ComponentId from the given number.
      *
      * @param id
      *   the number which will identify this ComponentId
      * @return
      *   a new ComponentId
      */
    private[ecscalibur] inline def apply(id: Int): ComponentId = id

    /** Creates a an array of ComponentIds from the given number sequence.
      *
      * @param ids
      *   the sequence of numbers which will identify the new ComponentIds
      * @return
      *   a new ComponentId sequence
      */
    private[ecscalibur] inline def apply(ids: Array[Int]): Array[ComponentId] = ids

  /** Base trait extended by [[Component]] and [[ComponentType]].
    */
  sealed trait WithType:
    protected val _typeId: Int = ComponentId.Nil

    /** The unique [[ComponentId]] that identifies the type of this component.
      *
      * @return
      *   this component's ComponentId
      */
    def typeId: ComponentId = if _typeId != ComponentId.Nil then ComponentId(_typeId)
    else throw IllegalDefinitionException(s"$getClass must be annotated with @component.")

    /** Equivalent to [[WithType.typeId]].
      *
      * @return
      *   the type ID of this component.
      */
    @targetName("typeId")
    inline def unary_~ : ComponentId = typeId

  /** Base trait that must be extended by all classes meant to be used as components.
    *
    * Classes extending this trait must be annotated with [[@component]] and also define a companion
    * object extending [[ComponentType]].
    */
  trait Component extends WithType:
    /** Checks whether this Component has the same ComponentId as the given [[ComponentType]].
      *
      * @param compType
      *   the ComponentType this Component will be tested against
      * @return
      *   true if this Component has the same ComponentId as the given type, false otherwise.
      */
    inline infix def isA(compType: ComponentType): Boolean = typeId == compType.typeId

  /** Base trait that must be extended by the companion objects of all classes meant to be used as
    * components.
    */
  trait ComponentType extends WithType:
    final override def equals(other: Any): Boolean = other match
      case o: WithType => typeId == o.typeId
      case _           => false
