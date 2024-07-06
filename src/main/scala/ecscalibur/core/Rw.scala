package ecscalibur.core

import ecscalibur.core.Entity
import ecscalibur.core.archetype.archetypes.Archetype
import ecscalibur.core.component.Component
import ecscalibur.core.component.ComponentType
import ecscalibur.core.component.tpe

import scala.annotation.targetName

// TODO Find out why this class in particular breaks @component
final class Rw[T <: Component](c: T)(archetype: Archetype, entity: Entity) extends Component:
  private var _component: T = c

  override protected val _typeId: Int = Rw._typeId

  inline def get: T = _component

  inline def apply(): T = _component

  @targetName("set")
  inline infix def <==(c: T): Unit =
    _component = c
    archetype.update(entity, c)

object Rw extends ComponentType:
  override protected val _typeId: Int = tpe.createId(getClass.getName)
