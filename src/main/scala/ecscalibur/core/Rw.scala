package ecscalibur.core

import ecscalibur.core.Entity
import ecscalibur.core.component.{Component, ComponentType, tpe}
import ecscalibur.core.archetype.Archetypes.Archetype

// TODO Find out why this class in particular breaks @component
final class Rw[T <: Component](c: T)(archetype: Archetype, entity: Entity) extends Component:
  var _component: T = c

  override protected val _typeId: Int = Rw._typeId

  inline def get : T = _component
  inline def apply(): T = _component
  inline infix def <==(c: T) =
    _component = c
    archetype.update(entity, c)

object Rw extends ComponentType:
  override protected val _typeId: Int = tpe.createId(getClass.getName)
