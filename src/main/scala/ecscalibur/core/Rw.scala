package ecscalibur.core

import ecscalibur.core.Entity
import ecscalibur.core.archetype.archetypes.Archetype
import ecscalibur.core.component.Component
import ecscalibur.core.component.ComponentType
import ecscalibur.core.component.tpe

import scala.annotation.targetName

// TODO Find out why this class in particular breaks @component

/** Wrapper for Components to be used in Queries. They make it possible to overwrite the value of an
  * Entity's Component.
  *
  * @param c
  *   the wrapped Component
  * @param archetype
  *   the Archetype where the wrapped Component is stored
  * @param entity
  *   the Entity related to the wrapped Component
  */
final class Rw[T <: Component](c: T)(archetype: Archetype, entity: Entity) extends Component:
  private var _component: T = c

  override protected val _typeId: Int = Rw._typeId

  /** @return
    *   a reference to the wrapped Component.
    */
  inline def get: T = _component

  /** @return
    *   a reference to the wrapped Component.
    */
  inline def apply(): T = _component

  /** Replaces the reference to the wrapped Component and updates the Archetype that stores it. 
    *
    * @param c
    *   the new Component value
    */
  @targetName("set")
  inline infix def <==(c: T): Unit =
    _component = c
    archetype.update(entity, c)

object Rw extends ComponentType:
  override protected val _typeId: Int = tpe.getId(getClass.getName)
